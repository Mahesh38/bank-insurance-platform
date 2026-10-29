// Generates the Login BRD v3.1 Postman collection + environments.
// Usage: node services/workforce-access-bff/postman/build-collection.js services/workforce-access-bff/postman
const fs = require('fs');
const path = require('path');
const out = process.argv[2];
if (!out) throw new Error('outDir required');

const AUTH = '{{baseUrl}}/api/v1/auth';
const lines = (s) => s.replace(/^\n/, '').split('\n');

// ---------------------------------------------------------------- collection scripts
const COLLECTION_PRE = `
// Workforce login collection: shared pre-request.
// Fills X-Correlation-Id, CSRF and captcha, and skips a request whose inputs are not available.
const envName = String(pm.variables.get('envName') || 'local').toLowerCase();
const baseUrl = pm.variables.get('baseUrl');
const requestName = pm.info.requestName;
const method = pm.request.method;
const raw = (pm.request.body && pm.request.body.raw) || '';

function blank(v) { return v === undefined || v === null || String(v).trim() === ''; }
function skip(reason) {
  console.info('SKIP "' + requestName + '": ' + reason);
  // A skipped step breaks its flow: drop the flow state so the next steps skip too,
  // instead of running with a challenge or token left by an earlier flow.
  ['challengeId', 'resetToken', 'usedResetToken', 'expiryTicket'].forEach(function (v) {
    pm.collectionVariables.unset(v);
  });
  pm.execution.skipRequest();
}

let skipped = false;
if (blank(baseUrl) || String(baseUrl).includes('REPLACE_ME')) {
  throw new Error('baseUrl is not set. Select an environment and set baseUrl.');
}

// [D] = changes account state (password counter, lock, password hash). Never on prod.
if (requestName.startsWith('[D]')) {
  if (envName === 'prod' || envName === 'production') {
    skipped = true; skip('account-changing request refused on prod');
  } else if (String(pm.variables.get('allowDestructive')) !== 'true') {
    skipped = true; skip('allowDestructive is not true for env ' + envName);
  }
}

// Local/dev-only requests.
if (!skipped && requestName.includes('(local/dev)') && !['local', 'dev'].includes(envName)) {
  skipped = true; skip('only runs on local or dev');
}

// A flow value an earlier step should have produced. otpCode is supplied by the environment.
const FLOW_VARS = ['otpCode', 'challengeId', 'resetToken', 'usedResetToken', 'expiryTicket',
  'sessionHandle', 'loggedOutHandle', 'usedCaptchaId', 'flsExpiredEmail', 'flsExpiredPassword',
  'flsExpiredNewPassword', 'bankRmEmployeeId', 'bankRmPassword', 'flsEmail',
  'flsPassword', 'flsFirstTimeEmail', 'flsFirstTimeNewPassword', 'flsTempPassword'];
if (!skipped) {
  const text = raw + ' ' + pm.request.url.toString() + ' ' +
    pm.request.headers.map(function (h) { return h.disabled ? '' : h.value; }).join(' ');
  for (const v of FLOW_VARS) {
    if (text.includes('{{' + v + '}}') && blank(pm.variables.get(v))) {
      skipped = true;
      skip(v === 'otpCode'
        ? 'otpCode is empty. On this env the OTP is not returned; set otpCode to the code the user received.'
        : v + ' is empty (an earlier step did not produce it, or the environment does not set it)');
      break;
    }
  }
}

if (!skipped) {
  pm.request.headers.upsert({ key: 'X-Correlation-Id', value: pm.variables.replaceIn('{{$guid}}') });

  const hasSessionHeader = pm.request.headers.has('X-Session-Handle');
  const wantsCsrf = method !== 'GET' && !hasSessionHeader && !requestName.includes('[no-csrf]');
  const wantsCaptcha = raw.includes('{{captchaId}}');

  function applyCsrf(includeHeader) {
    const cookie = pm.collectionVariables.get('csrfCookie');
    if (!blank(cookie)) pm.request.headers.upsert({ key: 'Cookie', value: cookie });
    if (includeHeader) {
      pm.request.headers.upsert({
        key: pm.collectionVariables.get('csrfHeaderName') || 'X-XSRF-TOKEN',
        value: pm.collectionVariables.get('csrfToken')
      });
    }
  }

  function fetchCsrf(next) {
    pm.sendRequest({ url: baseUrl + '/api/v1/auth/csrf', method: 'GET' }, function (err, res) {
      if (err) throw new Error('CSRF fetch failed: ' + err);
      if (res.code !== 200) throw new Error('CSRF fetch returned HTTP ' + res.code);
      const body = res.json();
      pm.collectionVariables.set('csrfToken', body.token);
      pm.collectionVariables.set('csrfHeaderName', body.headerName || 'X-XSRF-TOKEN');
      res.headers.each(function (h) {
        if (h.key.toLowerCase() === 'set-cookie' && h.value.indexOf('WORKFORCE-XSRF=') === 0) {
          pm.collectionVariables.set('csrfCookie', h.value.split(';')[0]);
        }
      });
      next();
    });
  }

  function fetchCaptcha(retried, next) {
    pm.sendRequest({
      url: baseUrl + '/api/v1/auth/captcha',
      method: 'POST',
      header: [
        { key: pm.collectionVariables.get('csrfHeaderName') || 'X-XSRF-TOKEN', value: pm.collectionVariables.get('csrfToken') },
        { key: 'Cookie', value: pm.collectionVariables.get('csrfCookie') || '' },
        { key: 'X-Correlation-Id', value: pm.variables.replaceIn('{{$guid}}') }
      ]
    }, function (err, res) {
      if (err) throw new Error('Captcha fetch failed: ' + err);
      if (res.code === 403 && !retried) {
        return fetchCsrf(function () { fetchCaptcha(true, next); });
      }
      if (res.code !== 200) throw new Error('Captcha fetch returned HTTP ' + res.code);
      const body = res.json();
      pm.collectionVariables.set('captchaId', body.captchaId);
      pm.collectionVariables.set('captchaValue', body.display);
      next();
    });
  }

  const needToken = (wantsCsrf || wantsCaptcha) && blank(pm.collectionVariables.get('csrfToken'));
  const finish = function () { applyCsrf(wantsCsrf); };
  const afterToken = function () { wantsCaptcha ? fetchCaptcha(false, finish) : finish(); };
  needToken ? fetchCsrf(afterToken) : afterToken();
}
`;

const COLLECTION_TEST = `
// Workforce login collection: shared checks and state capture after every response.
const requestName = pm.info.requestName;
const url = pm.request.url.getPath();
const status = pm.response.code;
const maxMs = Number(pm.variables.get('maxResponseMs') || 5000);
const text = pm.response.text();
let body = null;
try { body = text ? JSON.parse(text) : null; } catch (e) { body = null; }

pm.test('[common] responds within ' + maxMs + ' ms', function () {
  pm.expect(pm.response.responseTime).to.be.below(maxMs);
});

if (status !== 204) {
  pm.test('[common] body is JSON', function () { pm.expect(body, 'JSON body').to.not.equal(null); });
}

pm.test('[common] no provider token, password or OTP in the response', function () {
  pm.expect(text).to.not.match(/access_?token|refresh_?token|id_?token/i);
  ['bankRmPassword', 'flsPassword', 'flsTempPassword', 'flsFirstTimeNewPassword'].forEach(function (v) {
    const secret = pm.variables.get(v);
    if (secret && String(secret).length >= 6) pm.expect(text, v + ' echoed').to.not.include(secret);
  });
  if (body && typeof body === 'object') {
    ['otp', 'otpCode', 'password', 'newPassword', 'confirmPassword', 'accessToken', 'refreshToken', 'idToken']
      .forEach(function (k) { pm.expect(body, 'field ' + k).to.not.have.property(k); });
  }
});

if (status >= 400 && body) {
  pm.test('[common] error is a platform problem with a code', function () {
    pm.expect(body).to.have.property('code');
    pm.expect(body).to.have.property('status', status);
  });
  pm.test('[common] error carries no diagnostic detail', function () {
    pm.expect(body.diagnostic == null, 'diagnostic').to.equal(true);
    pm.expect(body.causeChain == null, 'causeChain').to.equal(true);
    pm.expect(body.origin == null, 'origin').to.equal(true);
  });
}

const OUTCOMES = ['MESSAGE', 'CAPTCHA', 'OTP_SENT', 'WAIT', 'CONTINUATION', 'AUTHENTICATED', 'COMPLETED',
  'PASSWORD_SET', 'PASSWORD_EXPIRED', 'NOT_REQUIRED'];
if (status === 200 && body && body.outcome !== undefined) {
  pm.test('[common] outcome is a v3.1 journey outcome', function () {
    pm.expect(OUTCOMES).to.include(body.outcome);
  });
  if (body.outcome === 'MESSAGE') {
    pm.test('[common] MESSAGE carries code and message', function () {
      pm.expect(body.code).to.be.a('string').and.not.empty;
      pm.expect(body.message).to.be.a('string').and.not.empty;
    });
  }
}

// Session cookie flags whenever the BFF sets one.
const setCookies = pm.response.headers.filter(function (h) { return h.key.toLowerCase() === 'set-cookie'; });
setCookies.forEach(function (h) {
  if (h.value.indexOf('WORKFORCE_SESSION=') === 0) {
    pm.test('[common] session cookie is HttpOnly, Secure, SameSite=Strict', function () {
      pm.expect(h.value).to.match(/HttpOnly/i);
      pm.expect(h.value).to.match(/Secure/i);
      pm.expect(h.value).to.match(/SameSite=Strict/i);
    });
    const value = h.value.split(';')[0].substring('WORKFORCE_SESSION='.length);
    if (value) pm.collectionVariables.set('sessionHandle', value);
  }
  if (h.value.indexOf('WORKFORCE-XSRF=') === 0) pm.collectionVariables.set('csrfCookie', h.value.split(';')[0]);
});

// ---- state capture
if (status === 403 && !requestName.includes('[no-csrf]')) {
  pm.collectionVariables.unset('csrfToken');   // next request fetches a fresh one
}
if (url.endsWith('/auth/csrf') && status === 200 && body) {
  pm.collectionVariables.set('csrfToken', body.token);
  pm.collectionVariables.set('csrfHeaderName', body.headerName || 'X-XSRF-TOKEN');
}
if (url.endsWith('/auth/captcha') && status === 200 && body) {
  pm.collectionVariables.set('previousCaptchaId', pm.collectionVariables.get('captchaId') || '');
  pm.collectionVariables.set('captchaId', body.captchaId);
  pm.collectionVariables.set('captchaValue', body.display);
}
const STARTERS = ['/auth/login', '/auth/unlock', '/auth/forgot-password', '/auth/password-expired/send-otp'];
if (STARTERS.some(function (p) { return url.endsWith(p); })) {
  // A captcha is single use; keep it so a reuse test can replay it.
  pm.collectionVariables.set('usedCaptchaId', pm.collectionVariables.get('captchaId') || '');
  pm.collectionVariables.set('usedCaptchaValue', pm.collectionVariables.get('captchaValue') || '');
  pm.collectionVariables.unset('captchaId');
  pm.collectionVariables.unset('captchaValue');
  if (body && body.challengeId) pm.collectionVariables.set('challengeId', body.challengeId);
  else pm.collectionVariables.unset('challengeId');
}
if (body && body.outcome === 'CONTINUATION') pm.collectionVariables.set('resetToken', body.continuationToken);
if (body && body.outcome === 'PASSWORD_EXPIRED') pm.collectionVariables.set('expiryTicket', body.continuationToken);
if (body && body.outcome === 'AUTHENTICATED') {
  if (body.sessionHandle) pm.collectionVariables.set('sessionHandle', body.sessionHandle);
  if (body.businessUserId) pm.collectionVariables.set('businessUserId', body.businessUserId);
}
if (url.endsWith('/auth/login') && status === 200 && body && body.sessionHandle) {
  pm.collectionVariables.set('sessionHandle', body.sessionHandle);
  pm.collectionVariables.set('businessUserId', body.businessUserId);
}
if (url.endsWith('/auth/password') && body && body.outcome === 'PASSWORD_SET') {
  pm.collectionVariables.set('usedResetToken', pm.collectionVariables.get('resetToken'));
  pm.collectionVariables.unset('resetToken');
}
if (url.endsWith('/auth/logout') && status === 204) {
  pm.collectionVariables.set('loggedOutHandle', pm.collectionVariables.get('sessionHandle'));
  pm.collectionVariables.unset('sessionHandle');
}
`;

// ---------------------------------------------------------------- test snippet helpers
const t = {
  status: (c) => `pm.test('HTTP ${c}', function () { pm.response.to.have.status(${c}); });`,
  statusIn: (cs) => `pm.test('HTTP one of ${cs.join('/')}', function () { pm.expect([${cs.join(',')}]).to.include(pm.response.code); });`,
  body: () => `const b = pm.response.json();`,
  outcome: (o) => `pm.test('outcome is ${o}', function () { pm.expect(b.outcome).to.eql('${o}'); });`,
  code: (c, field) => `pm.test('code ${c}${field ? ' on field ' + field : ''}', function () {
  pm.expect(b.outcome).to.eql('MESSAGE');
  pm.expect(b.code).to.eql('${c}');${field ? `\n  pm.expect(b.field).to.eql('${field}');` : ''}
});`,
  noSession: () => `pm.test('no session is created', function () {
  pm.expect(b.sessionHandle, 'sessionHandle').to.be.undefined;
  pm.expect(pm.response.headers.filter(function (h) { return h.key.toLowerCase() === 'set-cookie' && h.value.indexOf('WORKFORCE_SESSION=') === 0; })).to.be.empty;
});`,
  otpSent: (purpose) => `pm.test('OTP challenge issued (${purpose})', function () {
  pm.expect(b.outcome).to.eql('OTP_SENT');
  pm.expect(b.purpose).to.eql('${purpose}');
  pm.expect(b.challengeId).to.be.a('string').and.not.empty;
  pm.expect(b.expiresInSeconds).to.be.within(1, 600);
  pm.expect(b.resendAvailableInSeconds).to.be.within(0, 120);
  pm.expect(b.resendsRemaining).to.be.within(0, 3);
});
pm.test('mobile and email are masked', function () {
  if (b.maskedMobile) pm.expect(b.maskedMobile).to.not.match(/^\\+?\\d{10,}$/);
  if (b.maskedEmail) pm.expect(b.maskedEmail).to.not.eql(pm.variables.get('flsEmail'));
});`,
  authenticated: (userType) => `pm.test('AUTHENTICATED with a session', function () {
  pm.expect(b.outcome).to.eql('AUTHENTICATED');
  pm.expect(b.authenticated).to.eql(true);
  pm.expect(b.businessUserId).to.match(/^[0-9a-f-]{36}$/i);
  pm.expect(b.userType).to.eql('${userType}');
  pm.expect(b.redirectAfterSeconds).to.eql(2);
});
pm.test('session handle delivered for clientType ' + pm.variables.get('clientType'), function () {
  if (pm.variables.get('clientType') === 'NATIVE') pm.expect(b.sessionHandle).to.be.a('string').and.not.empty;
  else pm.expect(pm.response.headers.toString()).to.include('WORKFORCE_SESSION=');
});`,
};

// ---------------------------------------------------------------- request builders
const json = (o) => JSON.stringify(o, null, 2);
const H_JSON = { key: 'Content-Type', value: 'application/json' };
const H_SESSION = { key: 'X-Session-Handle', value: '{{sessionHandle}}' };

function req(name, method, pathOrUrl, { body, headers = [], tests = [], pre = [], description } = {}) {
  const url = pathOrUrl.startsWith('{{') ? pathOrUrl : AUTH + pathOrUrl;
  const r = {
    name,
    event: [],
    request: {
      method,
      header: [...(body !== undefined ? [H_JSON] : []), ...headers],
      url: { raw: url, host: [url.split('/')[0]], path: url.split('/').slice(1).filter(Boolean) },
    },
  };
  if (description) r.request.description = description;
  if (body !== undefined) {
    r.request.body = { mode: 'raw', raw: typeof body === 'string' ? body : json(body), options: { raw: { language: 'json' } } };
  }
  if (pre.length) r.event.push({ listen: 'prerequest', script: { type: 'text/javascript', exec: lines(pre.join('\n')) } });
  if (tests.length) r.event.push({ listen: 'test', script: { type: 'text/javascript', exec: lines(tests.join('\n')) } });
  return r;
}
const folder = (name, description, item) => ({ name, description, item });

// body fragments ({{captchaId}} triggers an automatic fresh captcha)
const signInBody = (userType, loginId, password, captchaValue = '{{captchaValue}}') =>
  `{\n  "clientType": "{{clientType}}",\n  "userType": "${userType}",\n  "loginId": "${loginId}",\n  "password": "${password}",\n  "captchaId": "{{captchaId}}",\n  "captchaValue": "${captchaValue}"\n}`;
const unlockBody = (userType, loginId) =>
  `{\n  "clientType": "{{clientType}}",\n  "userType": "${userType}",\n  "loginId": "${loginId}",\n  "captchaId": "{{captchaId}}",\n  "captchaValue": "{{captchaValue}}"\n}`;
const forgotBody = (email) =>
  `{\n  "clientType": "{{clientType}}",\n  "email": "${email}",\n  "captchaId": "{{captchaId}}",\n  "captchaValue": "{{captchaValue}}"\n}`;
const verifyBody = (otp = '{{otpCode}}', challenge = '{{challengeId}}') =>
  `{\n  "challengeId": "${challenge}",\n  "otp": "${otp}"\n}`;
const passwordBody = (pw, confirm = pw, token = '{{resetToken}}') =>
  `{\n  "resetToken": "${token}",\n  "newPassword": "${pw}",\n  "confirmPassword": "${confirm}"\n}`;

const ACCOUNTS = {
  bank: { label: 'Bank RM', userType: 'BANK_RM', loginId: '{{bankRmEmployeeId}}', password: '{{bankRmPassword}}', platformType: 'BANK_EMPLOYEE' },
  fls: { label: 'Insurance RM / FLS', userType: 'INSURANCE_RM', loginId: '{{flsEmail}}', password: '{{flsPassword}}', platformType: 'INSURER_REPRESENTATIVE' },
};

// Common sub-flows
const signInOtp = (n, a, { password = a.password, d = '' } = {}) =>
  req(`${d}${n} ${a.label} — sign in (captcha auto-filled)`, 'POST', '/login', {
    body: signInBody(a.userType, a.loginId, password),
    tests: [t.status(200), t.body(), t.otpSent('LOGIN'), t.noSession()],
  });
const verifyLogin = (n, a, d = '') =>
  req(`${d}${n} ${a.label} — verify login OTP`, 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), t.authenticated(a.platformType)],
  });
const readSession = (n, a) =>
  req(`${n} Read session (who am I)`, 'GET', '/session', {
    headers: [H_SESSION],
    tests: [t.status(200), t.body(), `pm.test('session belongs to the user who logged in', function () {
  pm.expect(b.businessUserId).to.eql(pm.collectionVariables.get('businessUserId'));
  pm.expect(b.userType).to.eql('${a.platformType}');
  pm.expect(b.status).to.eql('ACTIVE');
});`],
  });
const logout = (n, d = '') =>
  req(`${d}${n} Logout`, 'POST', '/logout', {
    headers: [H_SESSION],
    tests: [t.status(204), `pm.test('session cookie cleared', function () {
  const c = pm.response.headers.filter(function (h) { return h.key.toLowerCase() === 'set-cookie' && h.value.indexOf('WORKFORCE_SESSION=') === 0; });
  pm.expect(c).to.not.be.empty;
  pm.expect(c[0].value).to.match(/Max-Age=0/i);
});`],
  });
const loginAndLogout = (n, a, d = '', password) => [
  signInOtp(`${n}.1`, a, { d, password }),
  verifyLogin(`${n}.2`, a, d),
  logout(`${n}.3`, d),
];
const unlockClean = (n, a) => [
  req(`[D] ${n}.1 ${a.label} — unlock if locked`, 'POST', '/unlock', {
    body: unlockBody(a.userType, a.loginId),
    description: 'Self-healing pre-clean: a lock left by an interrupted run is cleared so later folders start from READY.',
    tests: [t.status(200), t.body(), `pm.test('unlock outcome is NOT_REQUIRED or OTP_SENT', function () {
  pm.expect(['NOT_REQUIRED', 'OTP_SENT']).to.include(b.outcome);
});
if (b.outcome === 'NOT_REQUIRED') console.info('${a.label} already unlocked');`],
  }),
  req(`[D] ${n}.2 ${a.label} — verify unlock OTP (only if one was sent)`, 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), `pm.test('unlock completed (or account was first-time)', function () {
  pm.expect(['COMPLETED', 'CONTINUATION']).to.include(b.outcome);
});`],
  }),
];

// ---------------------------------------------------------------- folders
const items = [];

items.push(folder('00 Setup', 'Health, CSRF, and a self-healing unlock of the test accounts (unlock is skipped unless allowDestructive=true).', [
  req('00.1 Health (local/dev)', 'GET', '{{baseUrl}}/actuator/health', {
    tests: [t.status(200), t.body(), `pm.test('BFF is UP', function () { pm.expect(b.status).to.eql('UP'); });`],
  }),
  req('00.2 Fetch CSRF token', 'GET', '/csrf', {
    tests: [t.status(200), t.body(), `pm.test('CSRF token contract', function () {
  pm.expect(b.headerName).to.eql('X-XSRF-TOKEN');
  pm.expect(b.token).to.be.a('string').and.not.empty;
});
pm.test('WORKFORCE-XSRF cookie set and readable by the app', function () {
  const c = pm.response.headers.filter(function (h) { return h.key.toLowerCase() === 'set-cookie' && h.value.indexOf('WORKFORCE-XSRF=') === 0; });
  if (c.length) pm.expect(c[0].value).to.not.match(/HttpOnly/i);
});`],
  }),
  ...unlockClean('00.3', ACCOUNTS.bank),
  ...unlockClean('00.4', ACCOUNTS.fls),
]));

items.push(folder('01 Captcha', 'Captcha issue, refresh, CSRF enforcement, wrong value and single use.', [
  req('01.1 Issue captcha', 'POST', '/captcha', {
    tests: [t.status(200), t.body(), `pm.test('captcha contract', function () {
  pm.expect(b.outcome).to.eql('CAPTCHA');
  pm.expect(b.captchaId).to.be.a('string').and.not.empty;
  pm.expect(b.display).to.be.a('string').and.have.length.within(4, 8);
  pm.expect(b.expiresInSeconds).to.be.within(1, 300);
});`],
  }),
  req('01.2 Refresh captcha gives a new id', 'POST', '/captcha', {
    tests: [t.status(200), t.body(), `pm.test('new captchaId on refresh', function () {
  pm.expect(b.captchaId).to.not.eql(pm.collectionVariables.get('previousCaptchaId'));
});`],
  }),
  req('01.3 Captcha without CSRF header is refused [no-csrf]', 'POST', '/captcha', {
    description: 'Scenario 01 §6 documents 403 CSRF_REJECTED in the platform problem shape.',
    tests: [t.status(403), `pm.test('403 is a platform problem with code CSRF_REJECTED', function () {
  let b = null; try { b = pm.response.json(); } catch (e) {}
  pm.expect(b && b.code).to.eql('CSRF_REJECTED');
});`],
  }),
  req('01.4 Sign-in with wrong captcha → VAL-004', 'POST', '/login', {
    body: signInBody('BANK_RM', '{{bankRmEmployeeId}}', '{{bankRmPassword}}', 'WRONG-{{$randomInt}}'),
    tests: [t.status(200), t.body(), t.code('VAL-004', 'captcha'), t.noSession()],
  }),
  req('01.5 Sign-in reusing a consumed captcha → VAL-004', 'POST', '/login', {
    body: `{\n  "clientType": "{{clientType}}",\n  "userType": "BANK_RM",\n  "loginId": "{{bankRmEmployeeId}}",\n  "password": "{{bankRmPassword}}",\n  "captchaId": "{{usedCaptchaId}}",\n  "captchaValue": "{{usedCaptchaValue}}"\n}`,
    description: 'Replays the captcha 01.4 consumed (a captcha is single use even when the answer was wrong).',
    tests: [t.status(200), t.body(), t.code('VAL-004', 'captcha')],
  }),
]));

items.push(folder('02 Bank RM login', 'Employee ID + network password (AD-verify) + login OTP → session → logout.', [
  signInOtp('02.1', ACCOUNTS.bank),
  verifyLogin('02.2', ACCOUNTS.bank),
  readSession('02.3', ACCOUNTS.bank),
  logout('02.4'),
  req('02.5 Session after logout is refused', 'GET', '/session', {
    headers: [{ key: 'X-Session-Handle', value: '{{loggedOutHandle}}' }],
    tests: [t.statusIn([400, 401])],
  }),
]));

items.push(folder('03 Insurance RM / FLS login', 'Corporate email + platform password + login OTP → session → logout. FLS is the INSURANCE_RM login.', [
  signInOtp('03.1', ACCOUNTS.fls),
  verifyLogin('03.2', ACCOUNTS.fls),
  readSession('03.3', ACCOUNTS.fls),
  logout('03.4'),
  req('03.5 Session after logout is refused', 'GET', '/session', {
    headers: [{ key: 'X-Session-Handle', value: '{{loggedOutHandle}}' }],
    tests: [t.statusIn([400, 401])],
  }),
]));

items.push(folder('04 Sign-in validation', 'Negative checks that do not change any account.', [
  req('04.1 Unknown Bank employee → VAL-005', 'POST', '/login', {
    body: signInBody('BANK_RM', 'NOSUCH{{$randomInt}}', 'Whatever#1'),
    tests: [t.status(200), t.body(), t.code('VAL-005', 'loginId'), t.noSession()],
  }),
  req('04.2 Unknown FLS email → VAL-006', 'POST', '/login', {
    body: signInBody('INSURANCE_RM', 'nobody.{{$randomInt}}@example.test', 'Whatever#1'),
    tests: [t.status(200), t.body(), t.code('VAL-006', 'loginId'), t.noSession()],
  }),
  req('04.3 First-time FLS sign-in → VAL-012', 'POST', '/login', {
    body: signInBody('INSURANCE_RM', '{{flsFirstTimeEmail}}', '{{flsFirstTimeNewPassword}}'),
    description: 'VAL-012 until folder 07 activates the account. After that the same request returns OTP_SENT (the password is the one 07 set, so no failure is counted).',
    tests: [t.status(200), t.body(), `pm.test('VAL-012 (first time) or OTP_SENT (already activated)', function () {
  if (b.outcome === 'MESSAGE') pm.expect(b.code).to.eql('VAL-012');
  else { pm.expect(b.outcome).to.eql('OTP_SENT'); console.info('first-time account already activated by an earlier run'); }
});`],
  }),
  req('04.4 Missing password → validation error', 'POST', '/login', {
    body: `{\n  "clientType": "{{clientType}}",\n  "userType": "BANK_RM",\n  "loginId": "{{bankRmEmployeeId}}",\n  "captchaId": "{{captchaId}}",\n  "captchaValue": "{{captchaValue}}"\n}`,
    tests: [t.statusIn([400, 422])],
  }),
  req('04.5 Unknown userType → validation error', 'POST', '/login', {
    body: signInBody('PARTNER_ADMIN', '{{bankRmEmployeeId}}', '{{bankRmPassword}}'),
    tests: [t.statusIn([400, 422])],
  }),
  req('04.6 Bank RM has no forgot-password: unknown email → VAL-023', 'POST', '/forgot-password', {
    body: forgotBody('{{bankRmEmployeeId}}@not-a-partner.example.test'),
    tests: [t.status(200), t.body(), t.code('VAL-023', 'email'), t.noSession()],
  }),
]));

items.push(folder('05 OTP verification', 'Wrong OTP, resend wait, success, replay, unknown challenge, attempt limit.', [
  signInOtp('05.1', ACCOUNTS.fls),
  req('05.2 Wrong OTP → VAL-014', 'POST', '/otp/verify', {
    body: verifyBody('000000'),
    tests: [t.status(200), t.body(), t.code('VAL-014', 'otp'), t.noSession()],
  }),
  req('05.3 Resend inside 2 minutes → WAIT', 'POST', '/otp/resend', {
    body: `{\n  "challengeId": "{{challengeId}}"\n}`,
    tests: [t.status(200), t.body(), `pm.test('WAIT with a countdown', function () {
  pm.expect(b.outcome).to.eql('WAIT');
  pm.expect(b.resendAvailableInSeconds).to.be.above(0);
  pm.expect(b.challengeId).to.eql(pm.collectionVariables.get('challengeId'));
});`],
  }),
  verifyLogin('05.4', ACCOUNTS.fls),
  req('05.5 Verify the same challenge again → VAL-015', 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), t.code('VAL-015', 'otp')],
  }),
  logout('05.6'),
  req('05.7 Unknown challenge → VAL-015', 'POST', '/otp/verify', {
    body: verifyBody('123456', '{{$guid}}'),
    tests: [t.status(200), t.body(), t.code('VAL-015', 'otp')],
  }),
  signInOtp('05.8', ACCOUNTS.fls),
  req('05.9 Wrong OTP five times → VAL-016', 'POST', '/otp/verify', {
    body: verifyBody('000000'),
    description: 'Repeats itself (setNextRequest) until the fifth wrong code. The account is not locked.',
    tests: [t.status(200), t.body(), `const n = Number(pm.collectionVariables.get('wrongOtpCount') || 0) + 1;
if (b.code === 'VAL-014' && n < 5) {
  pm.collectionVariables.set('wrongOtpCount', n);
  pm.test('attempt ' + n + ' → VAL-014', function () { pm.expect(b.code).to.eql('VAL-014'); });
  postman.setNextRequest(pm.info.requestName);
} else {
  pm.collectionVariables.unset('wrongOtpCount');
  pm.test('fifth wrong OTP → VAL-016', function () { pm.expect(b.code).to.eql('VAL-016'); });
}`],
  }),
  req('05.10 Challenge is gone after the limit → VAL-015', 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), t.code('VAL-015', 'otp')],
  }),
]));

function lockoutFolder(num, a) {
  const n = (k) => `${num}.${k}`;
  return folder(`${num} [D] ${a.label} lockout and unlock`, `Three wrong passwords lock ${a.label}; unlock with OTP restores it. Requires allowDestructive=true.`, [
    req(`[D] ${n(1)} Wrong password → VAL-008 (2 left)`, 'POST', '/login', {
      body: signInBody(a.userType, a.loginId, 'Wrong#{{$randomInt}}x'),
      tests: [t.status(200), t.body(), t.code('VAL-008', 'password')],
    }),
    req(`[D] ${n(2)} Wrong password → VAL-009 (1 left)`, 'POST', '/login', {
      body: signInBody(a.userType, a.loginId, 'Wrong#{{$randomInt}}x'),
      tests: [t.status(200), t.body(), t.code('VAL-009', 'password')],
    }),
    req(`[D] ${n(3)} Wrong password → VAL-010 locked`, 'POST', '/login', {
      body: signInBody(a.userType, a.loginId, 'Wrong#{{$randomInt}}x'),
      tests: [t.status(200), t.body(), t.code('VAL-010')],
    }),
    req(`[D] ${n(4)} Correct password while locked → VAL-010`, 'POST', '/login', {
      body: signInBody(a.userType, a.loginId, a.password),
      tests: [t.status(200), t.body(), t.code('VAL-010'), t.noSession()],
    }),
    ...(a.userType === 'INSURANCE_RM' ? [req(`[D] ${n(5)} Forgot password while locked → FORGOT_LOCKED`, 'POST', '/forgot-password', {
      body: forgotBody(a.loginId),
      tests: [t.status(200), t.body(), t.code('FORGOT_LOCKED')],
    })] : []),
    req(`[D] ${n(6)} Unlock → OTP_SENT (UNLOCK)`, 'POST', '/unlock', {
      body: unlockBody(a.userType, a.loginId),
      tests: [t.status(200), t.body(), t.otpSent('UNLOCK'), t.noSession()],
    }),
    req(`[D] ${n(7)} Verify unlock OTP → COMPLETED`, 'POST', '/otp/verify', {
      body: verifyBody(),
      tests: [t.status(200), t.body(), t.outcome('COMPLETED'), `pm.test('redirect after 3 s, no session', function () {
  pm.expect(b.redirectAfterSeconds).to.eql(3);
  pm.expect(b.sessionHandle).to.be.undefined;
});`],
    }),
    req(`[D] ${n(8)} Unlock again → NOT_REQUIRED`, 'POST', '/unlock', {
      body: unlockBody(a.userType, a.loginId),
      tests: [t.status(200), t.body(), t.outcome('NOT_REQUIRED')],
    }),
    ...loginAndLogout(n(9), a, '[D] '),
  ]);
}
items.push(lockoutFolder('06', ACCOUNTS.bank));
items.push(lockoutFolder('07', ACCOUNTS.fls));

const firstTime = { ...ACCOUNTS.fls, label: 'First-time FLS', loginId: '{{flsFirstTimeEmail}}', password: '{{flsFirstTimeNewPassword}}' };
items.push(folder('08 [D] First-time FLS activation', 'Unlock → OTP → set first password → sign in. On a rerun the account is already active: 08.1 returns NOT_REQUIRED and 08.2–08.3 skip themselves.', [
  req('[D] 08.1 Unlock first-time FLS → OTP_SENT', 'POST', '/unlock', {
    body: unlockBody('INSURANCE_RM', '{{flsFirstTimeEmail}}'),
    tests: [t.status(200), t.body(), `pm.test('OTP_SENT (UNLOCK), or NOT_REQUIRED when already activated', function () {
  pm.expect(['OTP_SENT', 'NOT_REQUIRED']).to.include(b.outcome);
  if (b.outcome === 'OTP_SENT') pm.expect(b.purpose).to.eql('UNLOCK');
});
pm.collectionVariables.unset('resetToken');`],
  }),
  req('[D] 08.2 Verify OTP → CONTINUATION (FIRST_TIME)', 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), `pm.test('continuation to set the first password', function () {
  pm.expect(b.outcome).to.eql('CONTINUATION');
  pm.expect(b.purpose).to.eql('FIRST_TIME');
  pm.expect(b.continuationToken).to.be.a('string').and.not.empty;
});`, t.noSession()],
  }),
  req('[D] 08.3 Set first password → PASSWORD_SET', 'POST', '/password', {
    body: passwordBody('{{flsFirstTimeNewPassword}}'),
    tests: [t.status(200), t.body(), t.outcome('PASSWORD_SET'), `pm.test('Account unlocked copy, redirect 3 s', function () {
  pm.expect(b.title).to.eql('Account unlocked');
  pm.expect(b.redirectAfterSeconds).to.eql(3);
});`, t.noSession()],
  }),
  ...loginAndLogout('08.4', firstTime, '[D] '),
]));

const rotated = { ...ACCOUNTS.fls, password: '{{flsTempPassword}}' };
const forgotSteps = (n, newPw, purposeTitle) => [
  req(`[D] ${n}.1 Forgot password → OTP_SENT (FORGOT)`, 'POST', '/forgot-password', {
    body: forgotBody('{{flsEmail}}'),
    tests: [t.status(200), t.body(), t.otpSent('FORGOT'), t.noSession()],
  }),
  req(`[D] ${n}.2 Verify forgot OTP → CONTINUATION`, 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), `pm.test('reset token issued', function () {
  pm.expect(b.outcome).to.eql('CONTINUATION');
  pm.expect(b.purpose).to.eql('FORGOT');
  pm.expect(b.continuationToken).to.be.a('string').and.not.empty;
});`, t.noSession()],
  }),
  req(`[D] ${n}.3 Set new password → PASSWORD_SET`, 'POST', '/password', {
    body: passwordBody(newPw),
    tests: [t.status(200), t.body(), t.outcome('PASSWORD_SET'), `pm.test('${purposeTitle}', function () { pm.expect(b.title).to.eql('Password changed'); });`, t.noSession()],
  }),
];
items.push(folder('09 [D] Forgot and reset password (FLS)', 'Rotates the FLS password to a generated temporary one, proves old/new behaviour, then restores the original so the account is left as found.', [
  req('[D] 09.0 Generate a temporary password', 'GET', '/csrf', {
    pre: [`// 8–16 chars, upper, lower, digit, special, no space, not the email.
const digits = String(Math.floor(1000 + Math.random() * 9000));
pm.collectionVariables.set('flsTempPassword', 'Rot#' + digits + 'xY');`],
    tests: [t.status(200)],
  }),
  ...forgotSteps('09.1', '{{flsTempPassword}}', 'Password changed'),
  req('[D] 09.2 Reuse the reset token → VAL-024', 'POST', '/password', {
    body: passwordBody('{{flsTempPassword}}', '{{flsTempPassword}}', '{{usedResetToken}}'),
    tests: [t.status(200), t.body(), t.code('VAL-024')],
  }),
  req('[D] 09.3 Old password no longer works → VAL-008', 'POST', '/login', {
    body: signInBody('INSURANCE_RM', '{{flsEmail}}', '{{flsPassword}}'),
    tests: [t.status(200), t.body(), t.code('VAL-008', 'password')],
  }),
  ...loginAndLogout('09.4', rotated, '[D] '),
  ...forgotSteps('09.5', '{{flsPassword}}', 'Password changed (restored)'),
  ...loginAndLogout('09.6', ACCOUNTS.fls, '[D] '),
]));

items.push(folder('10 [D] Set-password policy (data-driven)', 'One forgot-password journey per data row; each row submits one invalid password and expects its code. Run with data/set-password-policy.csv. Without a data file it runs the mismatch row.', [
  req('[D] 10.1 Forgot password → OTP_SENT', 'POST', '/forgot-password', {
    body: forgotBody('{{flsEmail}}'),
    tests: [t.status(200), t.body(), t.otpSent('FORGOT')],
  }),
  req('[D] 10.2 Verify forgot OTP → CONTINUATION', 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), t.outcome('CONTINUATION')],
  }),
  req('[D] 10.3 Submit invalid password → expected policy code', 'POST', '/password', {
    body: passwordBody('{{policyNewPassword}}', '{{policyConfirmPassword}}'),
    pre: [`// __CURRENT__ in the data file means "the account's current password".
['policyNewPassword', 'policyConfirmPassword'].forEach(function (k) {
  if (pm.variables.get(k) === '__CURRENT__') pm.variables.set(k, pm.variables.get('flsPassword'));
});`],
    tests: [t.status(200), t.body(), `const expected = pm.variables.get('expectedPolicyCode');
pm.test('case "' + (pm.variables.get('case') || 'mismatch') + '" → ' + expected, function () {
  pm.expect(b.outcome).to.eql('MESSAGE');
  pm.expect(b.code).to.eql(expected);
});
pm.test('password not changed: no PASSWORD_SET', function () { pm.expect(b.outcome).to.not.eql('PASSWORD_SET'); });`],
  }),
]));

items.push(folder('11 Insurance RM password expiry (conditional)', 'Runs only when the environment sets flsExpiredEmail / flsExpiredPassword to an account whose password is older than 60 days. Otherwise every request skips itself.', [
  req('[D] 11.1 Sign in with an expired password → PASSWORD_EXPIRED', 'POST', '/login', {
    body: signInBody('INSURANCE_RM', '{{flsExpiredEmail}}', '{{flsExpiredPassword}}'),
    tests: [t.status(200), t.body(), t.outcome('PASSWORD_EXPIRED'), `pm.test('expiry ticket, purpose EXPIRY', function () {
  pm.expect(b.continuationToken).to.be.a('string').and.not.empty;
  pm.expect(b.purpose).to.eql('EXPIRY');
});`, t.noSession()],
  }),
  req('[D] 11.2 Set password with the expiry ticket is refused → VAL-024', 'POST', '/password', {
    body: passwordBody('Exp#{{$randomInt}}Ab', 'Exp#{{$randomInt}}Ab', '{{expiryTicket}}'),
    description: 'The ticket from the expired screen is not a reset token; the OTP must be verified first. The ticket is consumed by this call, so 11.3 signs in again.',
    tests: [t.status(200), t.body(), t.code('VAL-024')],
  }),
  req('[D] 11.3 Sign in again → PASSWORD_EXPIRED', 'POST', '/login', {
    body: signInBody('INSURANCE_RM', '{{flsExpiredEmail}}', '{{flsExpiredPassword}}'),
    tests: [t.status(200), t.body(), t.outcome('PASSWORD_EXPIRED')],
  }),
  req('[D] 11.4 Send expiry OTP → OTP_SENT (EXPIRY)', 'POST', '/password-expired/send-otp', {
    body: `{\n  "expiryTicket": "{{expiryTicket}}"\n}`,
    tests: [t.status(200), t.body(), t.otpSent('EXPIRY')],
  }),
  req('[D] 11.5 Verify expiry OTP → CONTINUATION', 'POST', '/otp/verify', {
    body: verifyBody(),
    tests: [t.status(200), t.body(), t.outcome('CONTINUATION')],
  }),
  req('[D] 11.6 Set new password → PASSWORD_SET (valid 60 days)', 'POST', '/password', {
    body: passwordBody('{{flsExpiredNewPassword}}'),
    tests: [t.status(200), t.body(), t.outcome('PASSWORD_SET'), `pm.test('Password updated copy', function () { pm.expect(b.title).to.eql('Password updated'); });
// The expired password is now replaced; keep the env in step for the next run.
pm.environment.set('flsExpiredPassword', pm.variables.get('flsExpiredNewPassword'));`],
  }),
]));

items.push(folder('12 Retired IAM-001 shapes (SUG-20261028-1lg)', 'IAM-001 immediate-session /login is gone. Login BRD owns POST /login. Partner OIDC begins at POST /partner/authorization.', [
  req('12.1 Legacy BANK_AD body on /login → 422 VALIDATION_ERROR', 'POST', '/login', {
    body: `{\n  "clientType": "{{clientType}}",\n  "identitySource": "BANK_AD",\n  "employeeId": "{{bankRmEmployeeId}}",\n  "password": "{{bankRmPassword}}"\n}`,
    tests: [t.status(422), t.body(), `pm.test('VALIDATION_ERROR — BRD fields required', function () {
  pm.expect(b.code).to.eql('VALIDATION_ERROR');
});`],
  }),
  req('12.2 Partner begin on /partner/authorization → authorizationUri', 'POST', '/partner/authorization', {
    body: `{\n  "clientType": "{{clientType}}",\n  "returnUri": "{{partnerReturnUri}}"\n}`,
    tests: [t.status(200), t.body(), `pm.test('authorizationUri returned', function () {
  pm.expect(b.authorizationUri).to.be.a('string').and.not.empty;
  pm.expect(b.expiresInSeconds).to.be.a('number').and.above(0);
});`],
  }),
]));

const COLLECTION_VARS = [
  'csrfToken', 'csrfHeaderName', 'csrfCookie', 'captchaId', 'captchaValue', 'previousCaptchaId',
  'usedCaptchaId', 'usedCaptchaValue', 'challengeId', 'resetToken', 'usedResetToken', 'expiryTicket',
  'sessionHandle', 'loggedOutHandle', 'businessUserId', 'flsTempPassword', 'wrongOtpCount',
].map((key) => ({ key, value: '', type: 'string' }))
  .concat([
    { key: 'policyNewPassword', value: 'Valid#Pass1', type: 'string' },
    { key: 'policyConfirmPassword', value: 'Valid#Pass2', type: 'string' },
    { key: 'expectedPolicyCode', value: 'VAL-019', type: 'string' },
  ]);

const collection = {
  info: {
    name: 'Workforce Login — BRD v3.1',
    _postman_id: 'b3c1f0e2-5a4d-4c3b-9e8f-1a2b3c4d5e6f',
    description: [
      'Validates the workforce login API on workforce-access-bff (`/api/v1/auth/**`): Bank RM and Insurance RM/FLS sign-in, captcha, OTP, unlock, forgot and reset password, expiry, session and logout.',
      '',
      'Pick an environment (local, dev, uat, prod) and run the collection. Scripts fetch CSRF and a fresh captcha, carry challenge ids, reset tokens and session handles between requests, and assert every response.',
      '',
      '`[D]` requests change account state and run only when `allowDestructive=true` (never on prod). Requests needing an OTP skip themselves when `otpCode` is empty.',
      '',
      'Source of truth: docs/journey-execution/flows/LOGIN-BRD-V31-API-FLOWS.md. Work item SUG-20261025-pmn. See services/workforce-access-bff/postman/README.md.',
    ].join('\n'),
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  event: [
    { listen: 'prerequest', script: { type: 'text/javascript', exec: lines(COLLECTION_PRE) } },
    { listen: 'test', script: { type: 'text/javascript', exec: lines(COLLECTION_TEST) } },
  ],
  variable: COLLECTION_VARS,
  item: items,
};

// ---------------------------------------------------------------- environments
function env(name, id, values) {
  return {
    id, name: `Workforce Login — ${name}`,
    values: Object.entries(values).map(([key, [value, secret]]) => ({ key, value, type: secret ? 'secret' : 'default', enabled: true })),
    _postman_variable_scope: 'environment',
  };
}
const devAccounts = {
  bankRmEmployeeId: ['EMP001'],
  bankRmPassword: ['local-stub-password', true],
  flsEmail: ['fls.rm@example.test'],
  flsPassword: ['Horse#99', true],
  flsFirstTimeEmail: ['fls.new@example.test'],
  flsFirstTimeNewPassword: ['Pony#2026x', true],
};
const blankAccounts = {
  bankRmEmployeeId: [''], bankRmPassword: ['', true], flsEmail: [''], flsPassword: ['', true],
  flsFirstTimeEmail: [''], flsFirstTimeNewPassword: ['', true],
};
const expiry = { flsExpiredEmail: [''], flsExpiredPassword: ['', true], flsExpiredNewPassword: ['', true] };
const envs = {
  local: env('local', '7d0e5a10-0001-4a00-9000-000000000001', {
    envName: ['local'], baseUrl: ['http://localhost:8084'], clientType: ['NATIVE'],
    partnerReturnUri: ['bankinsurance://auth-complete'],
    otpCode: ['482913'], allowDestructive: ['true'], maxResponseMs: ['5000'], ...devAccounts, ...expiry,
  }),
  dev: env('dev', '7d0e5a10-0002-4a00-9000-000000000002', {
    envName: ['dev'], baseUrl: ['https://REPLACE_ME-dev-bff-host'], clientType: ['NATIVE'],
    partnerReturnUri: ['bankinsurance://auth-complete'],
    otpCode: ['482913'], allowDestructive: ['true'], maxResponseMs: ['5000'], ...devAccounts, ...expiry,
  }),
  uat: env('uat', '7d0e5a10-0003-4a00-9000-000000000003', {
    envName: ['uat'], baseUrl: ['https://uat-insurance.aubank.in'], clientType: ['NATIVE'],
    partnerReturnUri: ['bankinsurance://auth-complete'],
    otpCode: ['', true], allowDestructive: ['false'], maxResponseMs: ['5000'], ...blankAccounts, ...expiry,
  }),
  prod: env('prod', '7d0e5a10-0004-4a00-9000-000000000004', {
    envName: ['prod'], baseUrl: ['https://REPLACE_ME-prod-host'], clientType: ['NATIVE'],
    partnerReturnUri: ['bankinsurance://auth-complete'],
    otpCode: ['', true], allowDestructive: ['false'], maxResponseMs: ['5000'], ...blankAccounts, ...expiry,
  }),
};

fs.mkdirSync(path.join(out, 'environments'), { recursive: true });
fs.mkdirSync(path.join(out, 'data'), { recursive: true });
fs.writeFileSync(path.join(out, 'workforce-login.postman_collection.json'), JSON.stringify(collection, null, 2) + '\n');
for (const [k, v] of Object.entries(envs)) {
  fs.writeFileSync(path.join(out, 'environments', `${k}.postman_environment.json`), JSON.stringify(v, null, 2) + '\n');
}
fs.writeFileSync(path.join(out, 'data', 'set-password-policy.csv'), [
  'case,policyNewPassword,policyConfirmPassword,expectedPolicyCode',
  'mismatch,Valid#Pass1,Valid#Pass2,VAL-019',
  'default-123456,123456,123456,DEFAULT',
  'too-short,Ab#1xyz,Ab#1xyz,LENGTH',
  'too-long,Abcdefgh#12345678,Abcdefgh#12345678,LENGTH',
  'contains-space,Ab#1 xyzw,Ab#1 xyzw,SPACE',
  'no-uppercase,abcd#1234,abcd#1234,UPPER',
  'no-lowercase,ABCD#1234,ABCD#1234,LOWER',
  'no-number,Abcd#efgh,Abcd#efgh,NUMBER',
  'no-special,Abcd12345,Abcd12345,SPECIAL',
  'same-as-current,__CURRENT__,__CURRENT__,VAL-020',
].join('\n') + '\n');

// Syntax-check every script.
let count = 0;
(function walk(node) {
  (node.event || []).forEach((e) => { new Function('pm', 'postman', e.script.exec.join('\n')); count++; });
  (node.item || []).forEach(walk);
})(collection);
const reqCount = items.reduce((s, f) => s + f.item.length, 0);
console.log(`ok: ${items.length} folders, ${reqCount} requests, ${count} scripts compiled`);
