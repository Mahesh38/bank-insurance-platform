#!/usr/bin/env python3
"""R2 — answer the eight open instance questions in one run.

WHY THIS EXISTS
  CR-016's capability table was verified against GitLab's PUBLISHED DOCUMENTATION,
  not against gitlab-ce.au.bank.in. v19.1.2 is newer than the reachable docs, and
  the instance is authoritative over any published matrix. The agent cannot run
  this: the egress proxy denies CONNECT to the host (403, policy denial).

WHAT IT DOES
  Read-only by default. Answers what it can from the API, prints a summary, and
  writes instance-check.json for the register.

  Check 7 (subgroup creation) is the only WRITE, and it is opt-in via
  --allow-write. It creates a throwaway subgroup named
  zz-delete-me-r2-probe-<timestamp> and deletes it again. If deletion fails the
  script tells you the exact path to remove by hand.

USAGE
  export GITLAB_TOKEN=...            # a token that can read; add api scope for --allow-write
  python3 instance-check.py                        # read-only, safe
  python3 instance-check.py --allow-write          # also answers check 7
  python3 instance-check.py --project-id 1234      # probe against a real project

  Standard library only — no pip, no network beyond the instance itself.
"""
import argparse, json, os, ssl, sys, time, urllib.error, urllib.parse, urllib.request

BASE = os.environ.get("GITLAB_URL", "https://gitlab-ce.au.bank.in").rstrip("/")
TOKEN = os.environ.get("GITLAB_TOKEN", "")
PARENT_GROUP = int(os.environ.get("PARENT_GROUP_ID", "820"))

results, notes = {}, []


def call(method, path, data=None):
    """Returns (status, parsed_body_or_text). Never raises on HTTP status."""
    url = f"{BASE}/api/v4/{path.lstrip('/')}"
    body = urllib.parse.urlencode(data).encode() if data else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("PRIVATE-TOKEN", TOKEN)
    try:
        with urllib.request.urlopen(req, timeout=30, context=ssl.create_default_context()) as r:
            raw = r.read().decode("utf-8", "replace")
            try:
                return r.status, json.loads(raw)
            except json.JSONDecodeError:
                return r.status, raw
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, raw
    except Exception as e:
        return 0, f"{e.__class__.__name__}: {e}"


def record(key, question, answer, detail, decides):
    results[key] = {"question": question, "answer": answer, "detail": detail, "decides": decides}
    mark = {"YES": "[YES]", "NO": " [NO]", "?": "  [?]"}.get(answer, "  [?]")
    print(f"{mark}  {question}")
    print(f"       {detail}")
    print(f"       decides: {decides}\n")


def ee_available(status):
    """EE-only endpoints answer 403/404 on CE. 200 means the feature is present."""
    if status == 200:
        return "YES"
    if status in (401, 403, 404):
        return "NO"
    return "?"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--allow-write", action="store_true",
                    help="permit check 7 to create and delete a throwaway subgroup")
    ap.add_argument("--project-id", help="existing project to probe against (avoids creating one)")
    args = ap.parse_args()

    if not TOKEN:
        sys.exit("GITLAB_TOKEN is not set. Export it and re-run; it is never written to disk.")

    print(f"\nR2 instance check — {BASE}\n" + "=" * 72 + "\n")

    # --- identity and version -----------------------------------------------
    st, me = call("GET", "user")
    if st != 200:
        sys.exit(f"Cannot authenticate ({st}). Check the token and network path.\n{me}")
    st, ver = call("GET", "version")
    version = ver.get("version", "?") if isinstance(ver, dict) else "?"
    enterprise = ver.get("enterprise") if isinstance(ver, dict) else None
    print(f"authenticated as : {me.get('username')}")
    print(f"version reported : {version}   enterprise flag: {enterprise}\n")
    results["_meta"] = {"base": BASE, "version": version, "enterprise": enterprise,
                        "user": me.get("username"), "checked_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}

    pid = args.project_id
    if not pid:
        st, projs = call("GET", f"groups/{PARENT_GROUP}/projects?per_page=1")
        if st == 200 and isinstance(projs, list) and projs:
            pid = projs[0]["id"]
            notes.append(f"probed project-scoped checks against existing project id {pid}")
        else:
            notes.append("no project available — project-scoped checks are inconclusive; "
                         "re-run with --project-id once a project exists")

    # --- 4. Pipelines must succeed  (THE ONE THAT MUST BE YES) --------------
    if pid:
        st, p = call("GET", f"projects/{pid}")
        present = isinstance(p, dict) and "only_allow_merge_if_pipeline_succeeds" in p
        record("4_pipelines_must_succeed",
               'Is "Pipelines must succeed" available as a merge check?',
               "YES" if present else "NO",
               f"project {pid}: field {'present' if present else 'ABSENT'} "
               f"(value={p.get('only_allow_merge_if_pipeline_succeeds') if isinstance(p, dict) else '?'})",
               "S08-G2. If NO, the gate redesign in IMP-4 has no mechanism and CR-016 must be reopened.")
        # --- 6. Package Registry -------------------------------------------
        record("6_package_registry", "Is the Package Registry available?",
               "YES" if isinstance(p, dict) and "packages_enabled" in p else "?",
               f"packages_enabled={p.get('packages_enabled') if isinstance(p, dict) else '?'}",
               "ASM-017 second half. contracts and the shared libs are the consumers.")
        # --- 1. MR approval rules ------------------------------------------
        st, _ = call("GET", f"projects/{pid}/approval_rules")
        record("1_mr_approval_rules", "Do required merge-request approval rules exist?",
               ee_available(st), f"GET /projects/{pid}/approval_rules -> HTTP {st}",
               "Baseline 6.3, C-ARC-5, CR-016. Expected NO on CE.")
        # --- 3. Protected environments -------------------------------------
        st, _ = call("GET", f"projects/{pid}/protected_environments")
        record("3_protected_environments", "Are protected environments available?",
               ee_available(st), f"GET /projects/{pid}/protected_environments -> HTTP {st}",
               "Baseline 9.3, GLM-001 M6.6. Expected NO on CE.")
        # --- 8. Terraform state --------------------------------------------
        st, _ = call("GET", f"projects/{pid}/terraform/state")
        record("8_terraform_state", "Is GitLab-managed Terraform state available?",
               "YES" if st in (200, 204, 404) else "?",
               f"GET /projects/{pid}/terraform/state -> HTTP {st} "
               f"(404 here means 'no state yet', not 'unsupported')",
               "M1.6. NOTE SEC-F07: even if YES, bootstrap state must NOT live here — "
               "it controls the estate, so storing it inside the estate destroys its own recovery path.")
    else:
        for k, q in [("4_pipelines_must_succeed", 'Is "Pipelines must succeed" available?'),
                     ("6_package_registry", "Is the Package Registry available?"),
                     ("1_mr_approval_rules", "Do required MR approval rules exist?"),
                     ("3_protected_environments", "Are protected environments available?"),
                     ("8_terraform_state", "Is GitLab-managed Terraform state available?")]:
            record(k, q, "?", "no project available to probe", "re-run with --project-id")

    # --- 2. CODEOWNERS ------------------------------------------------------
    record("2_codeowners", "Does CODEOWNERS produce a REQUIRED approver?",
           results.get("1_mr_approval_rules", {}).get("answer", "?"),
           "CODEOWNERS enforcement rides on approval rules; inferred from check 1. "
           "Confirm in the UI: Settings > Merge requests > Approval rules.",
           "Baseline 6.2/6.4, C-ARC-5. If NO, a CODEOWNERS file is advisory only "
           "and section 6.4 must never be recorded as satisfied by its presence.")

    # --- 5. Security CI templates ------------------------------------------
    st, tpls = call("GET", "templates/gitlab_ci_ymls?per_page=100")
    names = [t.get("key", "") for t in tpls] if isinstance(tpls, list) else []
    sec = [n for n in names if "SAST" in n or "Secret" in n or "Dependency" in n or "Container" in n]
    record("5_security_templates", "Do the Security/*.gitlab-ci.yml templates resolve?",
           "YES" if sec else ("NO" if st == 200 else "?"),
           f"HTTP {st}; security-related templates found: {sec if sec else 'none'}",
           "S08-G5 mechanism. Absent templates are survivable — the ci-components sast job "
           "runs Semgrep directly and does not depend on them.")

    # --- 7. Subgroup creation  (THE ONE THAT GATES THE FIRST APPLY) ---------
    if args.allow_write:
        name = f"zz-delete-me-r2-probe-{int(time.time())}"
        st, created = call("POST", "groups",
                           {"name": name, "path": name, "parent_id": PARENT_GROUP, "visibility": "private"})
        if st in (200, 201) and isinstance(created, dict):
            gid = created.get("id")
            dst, _ = call("DELETE", f"groups/{gid}")
            cleaned = dst in (200, 202, 204)
            if not cleaned:
                notes.append(f"!! CLEAN UP BY HAND: group id {gid} path {created.get('full_path')} "
                             f"was created and NOT deleted (HTTP {dst})")
            record("7_subgroup_creation", f"Can this team create a subgroup under group {PARENT_GROUP}?",
                   "YES", f"created and {'deleted' if cleaned else 'FAILED TO DELETE'} {created.get('full_path')}",
                   "ASM-013 second half. GLM-001 M4.2/M4.3 — the FIRST APPLY. Nothing downstream runs without it.")
        else:
            record("7_subgroup_creation", f"Can this team create a subgroup under group {PARENT_GROUP}?",
                   "NO", f"POST /groups -> HTTP {st}: {str(created)[:200]}",
                   "ASM-013. A NO here blocks M4 entirely and needs a bank access request — "
                   "the longest lead time of any of the eight.")
    else:
        record("7_subgroup_creation", f"Can this team create a subgroup under group {PARENT_GROUP}?",
               "?", "skipped — this is the only write. Re-run with --allow-write.",
               "ASM-013 second half. THE HIGHEST-VALUE ANSWER: it gates the first apply.")

    # --- output -------------------------------------------------------------
    out = {"results": results, "notes": notes}
    with open("instance-check.json", "w") as f:
        json.dump(out, f, indent=2)

    print("=" * 72)
    for n in notes:
        print(f"note: {n}")
    ans = {k: v["answer"] for k, v in results.items() if not k.startswith("_")}
    print(f"\nanswered YES/NO: {sum(1 for v in ans.values() if v in ('YES', 'NO'))}/{len(ans)}   "
          f"inconclusive: {sum(1 for v in ans.values() if v == '?')}")
    print("\nWrote instance-check.json — paste it back, or attach it to ASM-012/013/017.")
    print("Nothing in that file is a secret; the token is never recorded.\n")


if __name__ == "__main__":
    main()
