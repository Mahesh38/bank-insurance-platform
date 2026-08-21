#!/usr/bin/env python3
"""R0 platform views — the pictures the AWS platform team provisions from.

Every coordinate here is chosen, not computed by a layout engine, and every
connector is a run of axis-aligned segments. See svgcanvas.py for why.

Rendering only (rule HA-02): every element is named by R0-LLD.md. Where this
file and R0-LLD.md disagree, R0-LLD.md wins and this file is the defect.

    python3 docs/architecture/diagrams/r0_platform_views.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from svgcanvas import Canvas                                    # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

# ------------------------------------------------------------------ palette
INK, MUTE = "#0f172a", "#64748b"
REQ, AUTH, EGR, STATE, MONEY, REPL = ("#1d4ed8", "#059669", "#b45309",
                                      "#475569", "#dc2626", "#0d9488")
Z = {  # zone: (stroke, fill)
    "dev":  ("#7e22ce", "#faf5ff"),
    "edge": ("#b45309", "#fffaf0"),
    "vpc":  ("#1e40af", "#f7fbff"),
    "app":  ("#2563eb", "#eff6ff"),
    "pub":  ("#ea580c", "#fff7ed"),
    "dat":  ("#475569", "#f8fafc"),
    "mgd":  ("#64748b", "#f8fafc"),
    "ext":  ("#dc2626", "#fffafa"),
    "dr":   ("#0d9488", "#f0fdfa"),
}
NS = {
    "edge":   ("#7e22ce", "#f7f5ff"),
    "ident":  ("#059669", "#f0fdf7"),
    "shared": ("#0369a1", "#f2f9ff"),
    "w1":     ("#0284c7", "#e4f2fe"),
    "w2":     ("#d97706", "#fef6e0"),
    "life":   ("#e11d48", "#fff4f6"),
    "integ":  ("#475569", "#f5f7fa"),
    "jobs":   ("#16a34a", "#f2fdf5"),
}

I = {  # icon paths, resolved from the diagrams wheel
    "r53":    "aws/network/route-53.png",
    "cf":     "aws/network/cloudfront.png",
    "apigw":  "aws/network/api-gateway.png",
    "alb":    "aws/network/elb-application-load-balancer.png",
    "nat":    "aws/network/nat-gateway.png",
    "igw":    "aws/network/internet-gateway.png",
    "vpc":    "aws/network/vpc.png",
    "subnet": "aws/network/private-subnet.png",
    "waf":    "aws/security/waf.png",
    "kms":    "aws/security/key-management-service.png",
    "secret": "aws/security/secrets-manager.png",
    "iam":    "aws/security/identity-and-access-management-iam-role.png",
    "eks":    "aws/compute/elastic-kubernetes-service.png",
    "ecr":    "aws/compute/ec2-container-registry.png",
    "aurora": "aws/database/aurora.png",
    "ddb":    "aws/database/dynamodb.png",
    "s3":     "aws/storage/simple-storage-service-s3.png",
    "backup": "aws/storage/backup.png",
    "cw":     "aws/management/cloudwatch.png",
    "amp":    "aws/management/amazon-managed-prometheus.png",
    "org":    "aws/management/organizations.png",
    "trail":  "aws/management/cloudtrail.png",
    "pod":    "k8s/compute/pod.png",
    "deploy": "k8s/compute/deploy.png",
    "cron":   "k8s/compute/cronjob.png",
    "mobile": "generic/device/mobile.png",
    "tablet": "generic/device/tablet.png",
    "flutter": "programming/framework/flutter.png",
    "net":    "onprem/network/internet.png",
    "argo":   "onprem/gitops/argocd.png",
}


def legend(c, x, y, w, rows, title="Legend"):
    h = 52 + len(rows) * 34
    c.group(title, x, y, w, h, stroke="#94a3b8", fill="#ffffff", label_size=15)
    for i, (color, dash, width, text) in enumerate(rows):
        ly = y + 62 + i * 34
        if color is None:                                   # a shape, not a line
            c.rect(x + 20, ly - 9, 46, 20, fill="#ffffff", stroke="#cbd5e1",
                   width=1.6, dash="6 5", radius=6, layer="node")
        else:
            c._defs_colors.add(color)
            da = ' stroke-dasharray="%s"' % dash if dash else ""
            c._add("node", '<path d="M%.1f,%.1f L%.1f,%.1f" stroke="%s" '
                           'stroke-width="%.1f" fill="none"%s marker-end="url(#ar%s)"/>'
                   % (x + 20, ly, x + 66, ly, color, width, da, color.lstrip("#")))
        c.text(x + 82, ly + 4.5, text, size=12.5, color=INK, anchor="start", halo=None)


# =========================================================================
# 1 — WHAT RUNS WHERE
# =========================================================================
# grid — every x below is one of these columns, every row pitch is a constant
COL = [830, 995, 1160, 1325, 1490, 1655]          # pod columns, 165 apart
NSX, NSW = 740, 1010                              # namespace band
ROW_H, ICON_DY = 165, 76                          # band height, icon centre in band
RIGHT_X, RIGHT_W = 1890, 360                      # the right-hand infrastructure column
RIGHT_CX = RIGHT_X + RIGHT_W / 2
LANE_EGRESS = 1830                                # corridor between app and public subnets


def topology():
    c = Canvas(2790, 2500,
               "R0 on AWS — what runs where",
               "ap-south-1 (Mumbai) · one VPC per environment · every connector is a real "
               "network path, and there are only two reverse proxies on it")

    # ---- devices ---------------------------------------------------------
    dev = c.group("DEVICES", 60, 305, 460, 195, stroke=Z["dev"][0], fill=Z["dev"][1],
                  sub="outside the VPC", label_size=15)
    c.node(I["flutter"], 190, 390, ["RM — Flutter app", "certified Specified Person"])
    c.node(I["tablet"], 390, 390, ["Insurance Partner Rep", "same host, same BFF"])

    # ---- region and edge -------------------------------------------------
    c.group("AWS REGION · ap-south-1", 620, 200, 1690, 2220, stroke=Z["vpc"][0],
            fill="#ffffff", dash="10 7", label_size=17, width=2.2)
    edge = c.group("PUBLIC AWS EDGE", 660, 240, 1610, 280, stroke=Z["edge"][0],
                   fill=Z["edge"][1], sub="the only internet entry point — not in the VPC",
                   label_size=16)
    cf = c.node(I["cf"], 760, 400, ["CloudFront", "TLS 1.3 · ACM"])
    waf = c.node(I["waf"], 960, 400, ["AWS WAF + Shield Std", "OWASP · rate limit"])
    agw = c.node(I["apigw"], 1160, 400, ["API Gateway", "PROXY 1 of 2", "throttle · schema"])
    pgcb = c.node(I["apigw"], 1480, 400, ["PG-callback route", "SEPARATE · IP-allowlisted"])
    c.node(I["r53"], 1780, 400, ["Route 53", "public + private zones", "a lookup, not a hop"])

    # ---- vpc -------------------------------------------------------------
    c.group("VPC · 10.{env}.0.0/16 · 3 Availability Zones", 660, 560, 1610, 1640,
            stroke=Z["vpc"][0], fill=Z["vpc"][1], label_size=17)
    c.group("PRIVATE-APP SUBNETS  /20 × 3 AZ", 700, 600, 1090, 1570, stroke=Z["app"][0],
            fill=Z["app"][1], sub="stateless pods · no PersistentVolumeClaim", label_size=15)
    alb = c.node(I["alb"], COL[2], 668, ["Internal ALB", "PROXY 2 of 2 — the only one in the VPC"])
    eks = c.group("Amazon EKS", 720, 760, 1050, 1380, stroke=Z["app"][0],
                  fill="#ffffff", label_size=15, opacity=0.55)

    def band(key, label, top, height=ROW_H, x=NSX, w=NSW):
        s_, f_ = NS[key]
        return c.group(label, x, top, w, height, stroke=s_, fill=f_,
                       label_size=13, radius=11, width=1.6)

    band("edge", "ns: edge", 815)
    bff = c.node(I["pod"], COL[2], 815 + ICON_DY, ["#2 RM Workspace BFF", "W4 · holds the tokens"])

    band("ident", "ns: identity   ·   WS-2 workforce identity", 995)
    c.node(I["deploy"], COL[0], 995 + ICON_DY, ["Keycloak", "private IdP · no PVC"])
    c.node(I["deploy"], COL[1], 995 + ICON_DY, ["identity-provider", "adapter"])
    pdp = c.node(I["deploy"], COL[2], 995 + ICON_DY, ["authorization PDP", "FAILS CLOSED"])

    band("shared", "ns: shared-platform   ·   one deployment for every line of business", 1175, 395)
    band("w1", "W0b + W1 — configuration and the journey spine", 1215, ROW_H, 758, 974)
    for i, rows in enumerate((["#19 Configuration", "W0b · fail closed"],
                              ["#5 Opportunity", "W1 · origination"],
                              ["#9 Journey Orch.", "W1 · state machine"],
                              ["#4 Customer", "W1 · ETB snapshot"],
                              ["#8 Product Catalogue", "W1 · Term only"])):
        c.node(I["pod"], COL[i], 1215 + ICON_DY, rows)
    band("w2", "W2 – W4 — the gate, the money, the evidence", 1390, ROW_H, 758, 974)
    for i, rows in enumerate((["#6 Consent", "W2 · append-only"],
                              ["#7 Suitability", "W2 · the hard gate"],
                              ["#12 Payment", "W3 · C4"],
                              ["#13 Policy", "W3 · iff RECONCILED"],
                              ["#16 Audit", "W3 · INSERT-only"],
                              ["#17 Notification", "W4 · OTP + pay-link"])):
        c.node(I["pod"], COL[i], 1390 + ICON_DY, rows)

    band("life", "ns: life-cell   ·   LOB-OWNED — Health gets its OWN copy later", 1585)
    c.node(I["pod"], COL[0], 1585 + ICON_DY, ["#10 Quotation (Life)", "W2 · needs suitability"])
    c.node(I["pod"], COL[1], 1585 + ICON_DY, ["#11 Proposal & UW", "W3 · no auto-retry"])

    band("integ", "ns: integration   ·   ALL provider traffic leaves here (SC-W3-5)", 1765)
    hub = c.node(I["pod"], COL[0], 1765 + ICON_DY, ["#14 Integration Hub", "distributorId server-side"])
    sb = c.node(I["pod"], COL[1], 1765 + ICON_DY, ["#15 1SB Adapter", "WS-1 · mTLS"])

    band("jobs", "ns: jobs", 1945)
    c.node(I["deploy"], COL[0], 1945 + ICON_DY, ["outbox-publisher", "×2 — no Kafka in R0"])
    c.node(I["cron"], COL[1], 1945 + ICON_DY, ["payment-reconcile", "issuance-recheck"])

    # ---- the right-hand infrastructure column ----------------------------
    c.group("PUBLIC SUBNETS  /24 × 3 AZ", RIGHT_X, 600, RIGHT_W, 370, stroke=Z["pub"][0],
            fill=Z["pub"][1], sub="NAT and IGW only — no workloads", label_size=14)
    nat = c.node(I["nat"], RIGHT_CX, 690, ["NAT Gateway + Elastic IP", "prod: one per AZ",
                                           "1SB and the PG allowlist THIS EIP"])
    igw = c.node(I["igw"], RIGHT_CX, 860, ["Internet Gateway", "for NAT egress only"])

    c.group("PRIVATE-DATA SUBNETS  /24 × 3 AZ", RIGHT_X, 1010, RIGHT_W, 250,
            stroke=Z["dat"][0], fill=Z["dat"][1], sub="no 0.0.0.0/0 route", label_size=14)
    aur = c.node(I["aurora"], RIGHT_CX, 1100, ["Aurora PostgreSQL — ONE cluster",
                                               "writer AZ-A + reader AZ-B",
                                               "16 schemas, one per context"])

    c.group("VPC ENDPOINTS", RIGHT_X, 1310, RIGHT_W, 300, stroke=Z["dat"][0],
            fill="#ffffff", sub="so none of this touches the internet", label_size=14)
    c.node(I["subnet"], RIGHT_CX, 1440, [], size=54)
    c.lines(RIGHT_CX, 1502, ["S3 · DynamoDB  (gateway)",
                             "ECR · Secrets Manager · KMS",
                             "CloudWatch · STS  (interface)"], size=12, color=INK)

    # ---- regional managed services --------------------------------------
    # label on the right: the two state connectors drop into this strip on the left
    c.group("AWS-MANAGED · REGIONAL", 700, 2240, 1550, 150, stroke=Z["mgd"][0],
            fill=Z["mgd"][1], sub="reached over the VPC endpoints — never the internet",
            label_size=14, label_align="right")
    s3 = c.node(I["s3"], COL[0], 2310, ["S3 + Object Lock", "7-year WORM"], size=54)
    ddb = c.node(I["ddb"], COL[1], 2310, ["DynamoDB + PITR", "journey · jobs"], size=54)
    for x, rows in ((1180, ["KMS CMK hierarchy"]), (1360, ["Secrets Manager"]),
                    (1540, ["ECR — by digest"]), (1720, ["CloudWatch · X-Ray"]),
                    (1900, ["AMP + AMG"]), (2080, ["Argo CD (in-cluster)"])):
        c.node(I["argo"] if x == 2080 else
               {1180: I["kms"], 1360: I["secret"], 1540: I["ecr"],
                1720: I["cw"], 1900: I["amp"]}.get(x), x, 2310, rows, size=54)

    # ---- outside ---------------------------------------------------------
    out = c.group("OUTSIDE", 2370, 620, 360, 340, stroke=Z["ext"][0], fill=Z["ext"][1],
                  sub="bank systems and insurance providers", label_size=15)
    c.node(I["net"], 2470, 710, ["Core Banking", "CBS / CIF"], size=54)
    pg = c.node(I["net"], 2650, 710, ["AU Bank", "Payment Gateway"], size=54)
    c.node(I["net"], 2470, 860, ["Bank AD / SSO", "WS-2 Phase 2"], size=54)
    c.node(I["net"], 2650, 860, ["1SilverBullet", "R0 polls"], size=54)

    # ---- connectors, all axis-aligned ------------------------------------
    c.link(dev.port("R", at=400), cf.port("L"), color=REQ, width=3.0)
    c.link(cf.port("R"), waf.port("L"), color=REQ, width=3.0)
    c.link(waf.port("R"), agw.port("L"), color=REQ, width=3.0)
    c.link(agw.port("B"), alb.port("T"), color=REQ, width=3.0,
           label="VPC link", label_at=0.62, label_dx=9, label_anchor="start")
    c.link(alb.port("B"), bff.port("T"), color=REQ, width=3.0)
    c.link(bff.port("B"), pdp.port("T"), color=AUTH, width=2.8)

    c.link(eks.port("R", at=1100), aur.port("L"), color=STATE, width=2.4, dash="2 5",
           label="JDBC", label_dy=-10)
    c.link(eks.port("B", at=COL[0]), s3.port("T"), color=STATE, width=2.4, dash="2 5")
    c.link(eks.port("B", at=COL[1]), ddb.port("T"), color=STATE, width=2.4, dash="2 5")

    c.link(hub.port("R"), sb.port("L"), color="#0369a1", width=2.4)
    c.link(sb.port("R"), nat.port("L"), color=EGR, width=2.8, dash="9 6",
           lane=LANE_EGRESS, label_seg=0, label_at=0.62,
           label=["the ONLY way out —", "every provider call"])
    c.link(nat.port("B"), igw.port("T"), color=EGR, width=2.8, dash="9 6")
    c.link(igw.port("R"), out.port("L", at=860), color=EGR, width=2.8, dash="9 6")
    c.link(pg.port("T"), pgcb.port("T"), color=MONEY, width=2.8, dash="9 6", lane=170,
           label="C4 payment callback — see the payment view")

    c.group("NOT IN R0 — do not provision", RIGHT_X, 1660, RIGHT_W, 300,
            stroke="#94a3b8", fill="#ffffff", label_size=14,
            sub="each of these is a decision, not an omission")
    c.lines(RIGHT_CX, 1745, [
        "MSK / Kafka — the outbox covers R0",
        "ElastiCache — no session store needed",
        "NLB · Transit Gateway · PrivateLink",
        "Service mesh — NetworkPolicy is enough",
        "A second live region — DR is warm standby",
        "Cognito — Keycloak is the R0 IdP",
    ], size=12, color=MUTE)

    legend(c, 2370, 1300, 360, [
        (REQ, None, 3.0, "Client request path"),
        (AUTH, None, 2.8, "Authorisation — on every call"),
        (EGR, "9 6", 2.8, "Egress — by the NAT Elastic IP"),
        (STATE, "2 5", 2.4, "Durable state"),
        (MONEY, "9 6", 2.8, "Payment callback (own view)"),
    ])
    c.text(2550, 1600, "Two reverse proxies. No more.", size=14, color=INK, bold=True)
    c.lines(2550, 1628, ["API Gateway is the only public one;",
                         "the internal ALB is the only one in the VPC.",
                         "Anything else on the path is a defect."], size=12.5, color=MUTE)
    return c.save(os.path.join(OUT, "r0-platform-topology.svg"))


# =========================================================================
# 2 — WHICH AVAILABILITY ZONE
# =========================================================================
def az():
    c = Canvas(2160, 1290, "Which availability zone",
               "ap-south-1 · pin AZ IDs (aps1-az1…), never the names — 'ap-south-1a' is a "
               "different physical zone in each AWS account")

    c.group("SHARED ACROSS ALL THREE ZONES", 60, 150, 2040, 170, stroke=Z["mgd"][0],
            fill=Z["mgd"][1], sub="regional services — there is nothing to place", label_size=15)
    for i, (ic, rows) in enumerate(((I["ecr"], ["ECR"]), (I["secret"], ["Secrets Manager"]),
                                    (I["kms"], ["KMS"]), (I["s3"], ["S3"]),
                                    (I["ddb"], ["DynamoDB"]))):
        c.node(ic, 300 + i * 200, 225, rows, size=52)
    c.lines(1620, 218, ["A regional service has no zone to choose.",
                        "Only the six resources below need one."], size=13, color=MUTE)

    zones = (("A", "dev · uat · prod", Z["app"][0], Z["app"][1], "full"),
             ("B", "uat · prod", Z["app"][0], Z["app"][1], "full"),
             ("C", "prod only  ·  uat: subnets, no paid capacity", "#94a3b8", "#f8fafc", "thin"))
    for i, (zid, envs, pen, bg, mode) in enumerate(zones):
        cx = 380 + i * 670
        c.group("AVAILABILITY ZONE  %s" % zid, cx - 320, 380, 640, 760, stroke=pen,
                fill=bg, sub=envs, label_size=18)

        c.group("public  /24", cx - 300, 465, 600, 180, stroke=Z["pub"][0],
                fill=Z["pub"][1], label_size=13, radius=11, width=1.6)
        if mode == "full":
            c.node(I["nat"], cx, 535, ["NAT Gateway + Elastic IP",
                                       "allowlisted by 1SB and the PG"], size=56)
        else:
            c.ghost(cx, 540, 340, 74, ["NAT + EIP — prod only",
                                       "a cost call: each EIP is one more to allowlist"])

        c.group("private-app  /20", cx - 300, 670, 600, 190, stroke=Z["app"][0],
                fill="#ffffff", label_size=13, radius=11, width=1.6)
        tail = "≥ 3 in uat/prod" if mode == "full" else "prod only"
        c.node(I["eks"], cx - 180, 745, ["EKS nodes", tail], size=56)
        c.node(I["alb"], cx, 745, ["Internal ALB", "one node here" if mode == "full" else "prod only"], size=56)
        if mode == "full":
            c.node(I["deploy"], cx + 180, 745, ["sale-path pods", "zone spread + PDB"], size=56)
        else:
            c.ghost(cx + 180, 750, 215, 66, ["sale-path pods", "prod only"])

        c.group("private-data  /24", cx - 300, 880, 600, 230, stroke=Z["dat"][0],
                fill=Z["dat"][1], label_size=13, radius=11, width=1.6)
        if zid == "A":
            c.node(I["aurora"], cx, 955, ["Aurora  WRITER",
                                           "single-AZ by definition"], size=56)
        elif zid == "B":
            c.node(I["aurora"], cx, 955, ["Aurora  READER",
                                           "MUST be a different AZ from the writer",
                                           "assert it in IaC — do not assume"], size=56)
        else:
            c.ghost(cx, 960, 340, 66, ["subnet reserved · no instance"])

    c.text(1080, 1230, "The only asymmetry that matters: the writer is in ONE zone. "
                       "Everything else is spread, and the spread is asserted, not hoped for.",
           size=14, color=INK)
    return c.save(os.path.join(OUT, "r0-platform-az.svg"))


# =========================================================================
# 3 — DISASTER RECOVERY
# =========================================================================
def dr():
    c = Canvas(2300, 1440, "Disaster recovery — warm standby",
               "RTO ≤ 1 h MEASURED · audit RPO 0 · ap-south-2 (Hyderabad) · "
               "active-active is explicitly not R0")

    c.group("PRIMARY — ap-south-1", 120, 200, 460, 1080, stroke=Z["app"][0],
            fill=Z["app"][1], sub="Mumbai · everything running", label_size=18)
    c.group("DR — ap-south-2", 900, 200, 1300, 1160, stroke=Z["dr"][0], fill=Z["dr"][1],
            sub="Hyderabad", label_size=18)
    c.group("PROVISIONED NOW — in the same change as the primary", 940, 270, 1220, 800,
            stroke="#0f766e", fill="#ccfbf1", label_size=15)
    c.group("NOT RUNNING — created or re-pointed at failover", 940, 1090, 1220, 230,
            stroke="#94a3b8", fill="#ffffff", label_size=15)

    pairs = (
        (I["ecr"], ["ECR"], ["D2  ECR replica"], "continuous"),
        (I["s3"], ["S3 + Object Lock"], ["D3  S3 replica", "Object Lock ON the replica too"], "CRR · RPO 0"),
        (I["aurora"], ["Aurora"], ["D4  Aurora DR", "Global secondary OR backup copy —",
                                   "Aarti decides, and the RTO is MEASURED"], "continuous"),
        (I["ddb"], ["DynamoDB"], ["D5  PITR is mandatory", "global tables = a cost decision"], "PITR"),
        (I["kms"], ["KMS CMKs"], ["D6  KMS replica keys"], "replica"),
        (I["secret"], ["Secrets Manager"], ["D7  Secrets replicas"], "replica"),
    )
    for i, (ic, lrows, rrows, tag) in enumerate(pairs):
        y = 360 + i * 125
        a = c.node(ic, 350, y, lrows, size=54)
        b = c.node(ic, 1080, y, rrows, size=54)
        c.link(a.port("R"), b.port("L"), color=REPL, width=2.6, dash="8 5",
               label=tag, label_size=11.5)

    eks_a = c.node(I["eks"], 350, 1190, ["EKS — running"], size=54)
    eks_b = c.node(I["eks"], 1080, 1190, ["D8  EKS", "node groups at 0"], size=54)
    c.link(eks_a.port("R"), eks_b.port("L"), color="#94a3b8", width=2.2, dash="4 5",
           label="NOT replicated", label_size=11.5)
    c.node(I["r53"], 1500, 1190, ["D9  Route 53 failover", "MANUAL in R0"], size=54)
    c.node(I["cf"], 1880, 1190, ["D10  CloudFront origin", "re-point — a runbook step"], size=54)

    c.node(I["vpc"], 1500, 380, ["D1  VPC + subnets", "empty · no NAT until failover"], size=54)
    c.node(I["iam"], 1880, 380, ["IAM roles + IaC", "the same modules, a different tfvars"], size=54)
    c.group("THE DELIVERABLE IS A RECORD, NOT A DESIGN", 1330, 560, 790, 260,
            stroke=Z["ext"][0], fill="#fef2f2", label_size=14)
    c.node(I["backup"], 1725, 660, [], size=54)
    c.lines(1725, 725, ["D11  a DR exercise, timed          (gate S09-G7)",
                        "D12  a rollback drill in UAT     (gate S09-G4)",
                        "An untested standby is a claim, not a capability."],
            size=12.5, color=INK)
    return c.save(os.path.join(OUT, "r0-platform-dr.svg"))


# =========================================================================
# 4 — WHEN
# =========================================================================
def sequence():
    bands = (
        ("P0", "GUARDRAILS", "before any resource exists", "#475569", "#f1f5f9",
         ((I["org"], ["5 accounts", "region SCP · TF state"]),
          (I["trail"], ["security account", "CloudTrail · Config"]),
          (I["kms"], ["CMK hierarchy"]))),
        ("P1", "NETWORK", "START HERE — longest external lead time", "#ea580c", "#fff7ed",
         ((I["vpc"], ["VPC · 3 AZ subnets"]),
          (I["nat"], ["NAT + ELASTIC IPs", "publish to 1SB and the PG"]),
          (I["subnet"], ["VPC endpoints"]))),
        ("P2", "COMPUTE", "", "#2563eb", "#eff6ff",
         ((I["eks"], ["EKS × 3 environments"]),
          (I["deploy"], ["admission policy", "NetworkPolicy default-deny"]),
          (I["iam"], ["IRSA per deployable"]))),
        ("P3", "DATA", "", "#475569", "#f8fafc",
         ((I["aurora"], ["Aurora + 16 schemas"]),
          (I["ddb"], ["DynamoDB + PITR"]),
          (I["s3"], ["S3 + OBJECT LOCK", "cannot be applied later"]))),
        ("P4", "EDGE + PROXY", "", "#b45309", "#fffaf0",
         ((I["alb"], ["Internal ALB"]),
          (I["apigw"], ["API Gateway", "+ PG callback — needed at W3"]),
          (I["cf"], ["CloudFront + WAF"]))),
        ("P5", "IDENTITY", "WS-2", "#059669", "#f0fdf7",
         ((I["deploy"], ["Keycloak + PDP"]),
          (I["secret"], ["Secrets Manager", "rotation exercised once"]))),
        ("P6", "OBSERVABILITY", "", "#7e22ce", "#faf5ff",
         ((I["amp"], ["AMP + AMG"]),
          (I["cw"], ["CloudWatch", "audit pipe SEPARATE"]))),
        ("P7", "DELIVERY", "", "#0369a1", "#f2f9ff",
         ((I["ecr"], ["ECR — built once"]),
          (I["argo"], ["Argo CD"]))),
        ("P8", "PROOF", "GATE-S09 accepts records, not designs", "#16a34a", "#f2fdf5",
         ((I["backup"], ["a restore, TIMED"]),
          (I["trail"], ["a rollback drill"]),
          (I["kms"], ["rotation exercised"]))),
    )
    w, gap, top, bh = 340, 34, 210, 530
    PITCH = 150
    c = Canvas(60 * 2 + len(bands) * w + (len(bands) - 1) * gap, 890,
               "When — the S09 provisioning sequence",
               "each band is gated on the one before it · P1 first because the Elastic IPs "
               "have to be allowlisted by two external parties")
    prev, anchor_y = None, top + 130
    for i, (code, name, sub, pen, bg, items) in enumerate(bands):
        x = 60 + i * (w + gap)
        g = c.group("%s  %s" % (code, name), x, top, w, bh, stroke=pen, fill=bg,
                    sub=sub or None, label_size=16)
        # centre the stack in the band so a two-item band is not bottom-heavy
        first = top + 70 + (bh - 110) / 2 - (len(items) - 1) * PITCH / 2
        for j, (ic, rows) in enumerate(items):
            c.node(ic, x + w / 2, first + j * PITCH, rows, size=56)
        if prev is not None:
            c.link(prev.port("R", at=anchor_y), g.port("L", at=anchor_y),
                   color=REQ, width=3.0)
        anchor_y = first
        prev = g
    c.text(c.w / 2, 830, "Nothing in P8 is a design document. Every item is a timed record "
                         "of something that actually ran.", size=14, color=INK)
    return c.save(os.path.join(OUT, "r0-platform-sequence.svg"))


# =========================================================================
# 5 — THE C4 PAYMENT PATH
# =========================================================================
def payment():
    c = Canvas(2520, 1160, "The C4 payment path — the hop people get wrong",
               "money is recovered by reconciliation, never by a database restore")

    c.group("INSIDE THE VPC", 80, 190, 940, 830, stroke=Z["app"][0], fill=Z["app"][1],
            label_size=17)
    pay = c.node(I["pod"], 540, 300, ["#12 Payment", "verifies the PG signature"])
    aur = c.node(I["aurora"], 400, 480, ["payment schema"], size=54)
    s3 = c.node(I["s3"], 830, 480, ["raw payload", "7-year WORM"], size=54)
    alb = c.node(I["alb"], 250, 700, ["Internal ALB"], size=56)
    rec = c.node(I["cron"], 540, 920, ["payment-reconcile", "S-15 · never auto-resolves"])
    pol = c.node(I["pod"], 860, 920, ["#13 Policy", "issues iff RECONCILED"])

    c.group("CUSTOMER", 1100, 190, 400, 260, stroke=Z["dev"][0], fill=Z["dev"][1],
            sub="not an RM device — FF-14", label_size=16)
    cust = c.node(I["mobile"], 1300, 300, ["Customer device", "3-D Secure happens here"])

    c.group("AU BANK — outside the platform boundary", 1600, 190, 820, 260,
            stroke=Z["ext"][0], fill=Z["ext"][1], label_size=16)
    pg = c.node(I["net"], 1800, 300, ["AU Bank Payment Gateway", "hosted page"], size=56)
    settle = c.node(I["net"], 2200, 300, ["settlement file", "arrives out-of-band"], size=56)

    c.group("OUR EDGE — a SEPARATE route from RM traffic (TB-6)", 1100, 580, 400, 240,
            stroke=Z["edge"][0], fill=Z["edge"][1], label_size=14)
    cb = c.node(I["apigw"], 1300, 700, ["PG-callback route", "IP-allowlisted to the PG"])

    c.link(pay.port("R"), cust.port("L"), color=MONEY, width=3.0,
           label="1   pay-link to the CUSTOMER device")
    c.link(cust.port("R"), pg.port("L"), color=MONEY, width=3.0,
           label="2   the RM never sees this URL")
    c.link(pg.port("B"), cb.port("T"), color=MONEY, width=3.0, lane=530,
           label="3   signed callback", label_seg=1)
    c.link(cb.port("L"), alb.port("R"), color=MONEY, width=3.0,
           label="4   never on the RM session")
    c.link(alb.port("T"), pay.port("L"), color=MONEY, width=3.0,
           label="5   verify the signature", label_seg=0, label_at=0.42)
    c.link(settle.port("B"), rec.port("T"), color=EGR, width=2.8, dash="9 6", lane=860,
           label="6   settlement, out-of-band", label_seg=1)
    c.link(rec.port("R"), pol.port("L"), color=AUTH, width=3.0, label="7   RECONCILED")
    c.link(pay.port("B"), aur.port("T"), color=STATE, width=2.4, dash="2 5", lane=418)
    c.link(pay.port("B"), s3.port("T"), color=STATE, width=2.4, dash="2 5", lane=434)

    c.text(1760, 950, "Why this has its own view", size=15, color=INK, bold=True)
    c.lines(1760, 985, [
        "Three things on this path are routinely got wrong:",
        "the pay-link goes to the CUSTOMER, not the RM device;",
        "the callback arrives on its own IP-allowlisted route, not the RM one;",
        "and a policy is issued only after RECONCILED — never on the callback alone.",
    ], size=13, color=MUTE)
    return c.save(os.path.join(OUT, "r0-platform-payment.svg"))


def rasterise(svg_path, width=2400):
    """PNG companion, for slide decks and tools that will not take an SVG.
    Optional: the SVG is the deliverable, this is a convenience."""
    try:
        import cairosvg
    except ImportError:
        return None
    png = svg_path[:-4] + ".png"
    cairosvg.svg2png(url=svg_path, write_to=png, output_width=width)
    return png


if __name__ == "__main__":
    for fn in (topology, az, dr, sequence, payment):
        svg = fn()
        png = rasterise(svg)
        print(os.path.normpath(svg) + ("  +png" if png else "  (no cairosvg: SVG only)"))
