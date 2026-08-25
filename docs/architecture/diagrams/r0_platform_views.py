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
    "tgw":    "aws/network/transit-gateway.png",
    "tgwa":   "aws/network/transit-gateway-attachment.png",
    "nfw":    "aws/network/network-firewall.png",
    "dx":     "aws/network/direct-connect.png",
    "vpn":    "aws/network/site-to-site-vpn.png",
    "cache":  "aws/database/elasticache.png",
    "msk":    "aws/analytics/managed-streaming-for-kafka.png",
    "srch":   "aws/analytics/amazon-opensearch-service.png",
    "fire":   "aws/analytics/kinesis-data-firehose.png",
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
    c = Canvas(3260, 2500,
               "R0 on AWS — what runs where",
               "ap-south-1 (Mumbai) · one workload VPC per environment, one inspection VPC per "
               "environment · every connector is a real network path, and egress has exactly one")

    # ---- devices ---------------------------------------------------------
    dev = c.group("DEVICES", 40, 250, 560, 270, stroke=Z["dev"][0], fill=Z["dev"][1],
                  sub="outside the VPC", label_size=15)
    c.node(I["flutter"], 130, 340, ["NIP-APP native", "APK Play · IPA App Store"])
    c.node(I["tablet"], 310, 340, ["NIP-APP web", "role-based · nip-web"])
    c.node(I["tablet"], 220, 455, ["Roles, not apps", "RM · IPR · admin/ops"])

    # ---- region and edge -------------------------------------------------
    c.group("AWS REGION · ap-south-1", 620, 200, 2160, 2220, stroke=Z["vpc"][0],
            fill="#ffffff", dash="10 7", label_size=17, width=2.2)
    edge = c.group("BANK PERIMETER & PUBLIC AWS EDGE", 660, 240, 1610, 280, stroke=Z["edge"][0],
                   fill=Z["edge"][1], sub="Cloudflare · F5 · External ALB · API Gateway — not in the VPC",
                   label_size=16)
    cf = c.node(I["cf"], 740, 400, ["Cloudflare", "Edge CDN · DDoS", "bank standard"])
    waf = c.node(I["waf"], 910, 400, ["F5 BIG-IP / WAF", "L7 security policy", "bank standard"])
    ext_alb = c.node(I["alb"], 1080, 400, ["External ALB", "Edge ingress"])
    agw = c.node(I["apigw"], 1250, 400, ["API Gateway", "PROXY 1 of 2", "throttle · schema"])
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

    band("edge", "ns: edge   ·   UI pods are image-baked — no PVC", 815)
    c.node(I["pod"], COL[0], 815 + ICON_DY, ["nip-web", "Flutter web · in the image"])
    bff = c.node(I["pod"], COL[1], 815 + ICON_DY, ["#2 NIP BFF", "W4 · holds the tokens"])

    band("ident", "ns: identity   ·   WS-2 workforce identity", 995)
    c.node(I["deploy"], COL[0], 995 + ICON_DY, ["Keycloak", "private IdP · no PVC"])
    c.node(I["deploy"], COL[1], 995 + ICON_DY, ["identity-provider", "adapter"])
    pdp = c.node(I["deploy"], COL[2], 995 + ICON_DY, ["authorization PDP", "FAILS CLOSED"])

    band("shared", "ns: shared-platform   ·   one deployment for every line of business", 1175, 395)
    band("w1", "W0b + W1 — configuration and the journey spine", 1215, ROW_H, 758, 974)
    for i, rows in enumerate((["#19 Configuration", "W0b · fail closed"],
                              ["#5 Lead", "W1 · origination"],
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
    c.node(I["deploy"], COL[0], 1945 + ICON_DY, ["outbox-publisher", "×2 — outbox → MSK"])
    c.node(I["cron"], COL[1], 1945 + ICON_DY, ["payment-reconcile", "issuance-recheck"])
    c.node(I["deploy"], COL[2], 1945 + ICON_DY, ["MSK consumers", "audit · notification",
                                                 "KEDA on lag"])
    c.node(I["deploy"], COL[3], 1945 + ICON_DY, ["#18 Reporting/MIS", "isolated read path",
                                                 "NEVER the Lead writer"])

    # ---- the right-hand infrastructure column ----------------------------
    c.group("PUBLIC SUBNETS  /24 × 3 AZ", RIGHT_X, 600, RIGHT_W, 200, stroke=Z["pub"][0],
            fill=Z["pub"][1], sub="reserved and EMPTY — no NAT here", label_size=14)
    c.ghost(RIGHT_CX, 720, 320, 72, ["no NAT · no IGW in the workload VPC",
                                     "egress is centralised — ADR-010"])

    c.group("TGW ATTACHMENT  /28 × 3 AZ", RIGHT_X, 830, RIGHT_W, 160, stroke=Z["vpc"][0],
            fill="#ffffff", sub="the only way out", label_size=14)
    tgwa = c.node(I["tgwa"], RIGHT_CX, 895, ["one ENI per AZ"], size=54)

    c.group("PRIVATE-DATA SUBNETS  /24 × 3 AZ", RIGHT_X, 1020, RIGHT_W, 620,
            stroke=Z["dat"][0], fill=Z["dat"][1], sub="no 0.0.0.0/0 route", label_size=14)
    aur = c.node(I["aurora"], RIGHT_CX, 1110, ["Aurora PostgreSQL — ONE cluster",
                                               "writer AZ-A + reader AZ-B",
                                               "16 schemas, one per context"], size=54)
    cache = c.node(I["cache"], RIGHT_CX, 1260, ["ElastiCache for Valkey",
                                                "sessions · L2 · rate limits",
                                                "NEVER idempotency"], size=54)
    msk = c.node(I["msk"], RIGHT_CX, 1410, ["Amazon MSK — 3 brokers",
                                            "outbox-fed transport",
                                            "NEVER the audit record"], size=54)
    srch = c.node(I["srch"], RIGHT_CX, 1560, ["OpenSearch — VPC only",
                                              "operational logs · 90 d",
                                              "NEVER evidence"], size=54)

    c.group("VPC ENDPOINTS", RIGHT_X, 1690, RIGHT_W, 270, stroke=Z["dat"][0],
            fill="#ffffff", sub="so none of this touches the internet", label_size=14)
    c.node(I["subnet"], RIGHT_CX, 1810, [], size=54)
    c.lines(RIGHT_CX, 1872, ["S3 · DynamoDB  (gateway)",
                             "ECR · Secrets Manager · KMS",
                             "CloudWatch · STS  (interface)"], size=12, color=INK)

    # ---- inspection / egress VPC — the network account -------------------
    insp = c.group("INSPECTION / EGRESS VPC", 2350, 560, 380, 560, stroke=Z["pub"][0],
                   fill=Z["pub"][1],
                   sub="network account · ONE PER ENVIRONMENT", label_size=15)
    tgw = c.node(I["tgw"], 2540, 650, ["Transit Gateway", "one route table per env",
                                       "no VPC peering, anywhere"], size=56)
    nfw = c.node(I["nfw"], 2540, 830, ["AWS Network Firewall", "domain allowlist · IPS",
                                       "one endpoint per AZ"], size=56)
    nat = c.node(I["nat"], 2540, 1010, ["NAT + ELASTIC IPs", "1SB and the PG allowlist THESE",
                                        "they MOVED here — ADR-010"], size=56)

    # ---- regional managed services --------------------------------------
    # label on the right: the two state connectors drop into this strip on the left
    c.group("AWS-MANAGED · REGIONAL", 700, 2240, 1550, 150, stroke=Z["mgd"][0],
            fill=Z["mgd"][1], sub="reached over the VPC endpoints — never the internet",
            label_size=14, label_align="right")
    s3 = c.node(I["s3"], COL[0], 2310, ["S3 + Object Lock", "7-year WORM"], size=54)
    ddb = c.node(I["ddb"], COL[1], 2310, ["DynamoDB + PITR", "journey · jobs"], size=54)
    for x, rows in ((1180, ["KMS CMK hierarchy"]), (1360, ["Secrets Manager"]),
                    (1540, ["ECR — by digest"]), (1720, ["CloudWatch + CloudTrail"]),
                    (1900, ["AMP + AMG"]), (2080, ["Argo CD (in-cluster)"])):
        c.node(I["argo"] if x == 2080 else
               {1180: I["kms"], 1360: I["secret"], 1540: I["ecr"],
                1720: I["cw"], 1900: I["amp"]}.get(x), x, 2310, rows, size=54)

    # ---- outside ---------------------------------------------------------
    out = c.group("OUTSIDE", 2840, 620, 360, 340, stroke=Z["ext"][0], fill=Z["ext"][1],
                  sub="bank systems and insurance providers", label_size=15)
    cbs = c.node(I["net"], 2940, 710, ["EBS (CBS / CIF)", "Enterprise Service Bus"], size=54)
    pg = c.node(I["net"], 3120, 710, ["AU Bank", "Payment Gateway"], size=54)
    c.node(I["net"], 2940, 860, ["Bank AD / SSO", "WS-2 Phase 2"], size=54)
    onesb = c.node(I["net"], 3120, 860, ["1SilverBullet", "R0 polls"], size=54)

    # ---- connectors, all axis-aligned ------------------------------------
    c.link(dev.port("R", at=380), cf.port("L"), color=REQ, width=3.0)
    c.link(cf.port("R"), waf.port("L"), color=REQ, width=3.0)
    c.link(waf.port("R"), ext_alb.port("L"), color=REQ, width=3.0)
    c.link(ext_alb.port("R"), agw.port("L"), color=REQ, width=3.0)
    c.link(agw.port("B"), alb.port("T"), color=REQ, width=3.0,
           label="VPC link", label_at=0.62, label_dx=9, label_anchor="start")
    c.link(alb.port("B"), bff.port("T"), color=REQ, width=3.0,
           label="GET /* → nip-web  ·  /api/* → NIP BFF", label_at=0.45, label_dx=8,
           label_anchor="start")
    c.link(bff.port("B"), pdp.port("T"), color=AUTH, width=2.8)

    c.link(eks.port("R", at=1110), aur.port("L"), color=STATE, width=2.4, dash="2 5",
           label="JDBC", label_dy=-10)
    c.link(eks.port("R", at=1260), cache.port("L"), color=STATE, width=2.4, dash="2 5",
           label="sessions · L2", label_dy=-10)
    c.link(eks.port("R", at=1410), msk.port("L"), color=STATE, width=2.4, dash="2 5",
           label="outbox → topic", label_dy=-10)
    c.link(eks.port("R", at=1560), srch.port("L"), color=MUTE, width=2.0, dash="2 5",
           label="logs only", label_dy=-10)
    c.link(eks.port("B", at=COL[0]), s3.port("T"), color=STATE, width=2.4, dash="2 5")
    c.link(eks.port("B", at=COL[1]), ddb.port("T"), color=STATE, width=2.4, dash="2 5")

    c.link(hub.port("R"), sb.port("L"), color="#0369a1", width=2.4)
    c.link(sb.port("R"), tgwa.port("L"), color=EGR, width=2.8, dash="9 6",
           lane=LANE_EGRESS, label_seg=0, label_at=0.62,
           label=["the ONLY way out —", "every provider call"])
    c.link(tgwa.port("R"), tgw.port("L"), color=EGR, width=2.8, dash="9 6", lane=2300)
    c.link(tgw.port("B"), nfw.port("T"), color=EGR, width=2.8, dash="9 6",
           label="inspected", label_dx=8, label_anchor="start")
    c.link(nfw.port("B"), nat.port("T"), color=EGR, width=2.8, dash="9 6")
    c.link(nat.port("R"), onesb.port("B"), color=EGR, width=2.8, dash="9 6",
           label="internet — by the Elastic IP", label_seg=1, label_at=0.72,
           label_dx=9, label_anchor="start")
    c.link(tgw.port("R"), cbs.port("L"), color=AUTH, width=2.8, dash="9 6", lane=2790,
           label_seg=1, label_at=0.5, label=["TB-7", "VPN now,", "DX next"],
           label_size=11.5)
    c.link(pg.port("T"), pgcb.port("T"), color=MONEY, width=2.8, dash="9 6", lane=170,
           label="C4 payment callback — see the payment view")

    c.group("NOT IN R0 — do not provision", 2840, 1010, 360, 300,
            stroke="#94a3b8", fill="#ffffff", label_size=14,
            sub="each of these is a decision, not an omission")
    c.lines(3020, 1095, [
        "Service mesh — NetworkPolicy + IRSA is enough",
        "A cluster per service — ADR-008 says one",
        "Glue ETL · Athena · Redshift · QuickSight",
        "  (#18 MIS is in R0 — this is the warehouse)",
        "MSK Replicator — DR replays the outbox",
        "Cache as an idempotency store — ADR-011",
        "OpenSearch as the audit store — ADR-013",
        "A second live region — DR is warm standby",
        "Cognito — Keycloak is the R0 IdP",
        "Pipelines — GitLab CI/CD is bank standard",
        "IaC — Terraform is the IaC baseline",
    ], size=12, color=MUTE)

    legend(c, 2840, 1370, 360, [
        (REQ, None, 3.0, "Client request path"),
        (AUTH, None, 2.8, "Authorisation · bank private path"),
        (EGR, "9 6", 2.8, "Egress — inspected, by the EIP"),
        (STATE, "2 5", 2.4, "Durable state"),
        (MONEY, "9 6", 2.8, "Payment callback (own view)"),
    ])
    c.text(3020, 1680, "Two reverse proxies. One way out.", size=14, color=INK, bold=True)
    c.lines(3020, 1708, ["API Gateway is the only public proxy;",
                         "the internal ALB is the only one in the VPC.",
                         "The firewall is on egress, not on ingress —",
                         "it terminates no client session.",
                         "Anything else on the path is a defect."], size=12.5, color=MUTE)
    return c.save(os.path.join(OUT, "r0-platform-topology.svg"))


# =========================================================================
# 2 — WHICH AVAILABILITY ZONE
# =========================================================================
def az():
    c = Canvas(2160, 1560, "Which availability zone",
               "ap-south-1 · pin AZ IDs (aps1-az1…), never the names — 'ap-south-1a' is a "
               "different physical zone in each AWS account")

    c.group("SHARED ACROSS ALL THREE ZONES", 60, 150, 2040, 170, stroke=Z["mgd"][0],
            fill=Z["mgd"][1], sub="regional services — there is nothing to place", label_size=15)
    for i, (ic, rows) in enumerate(((I["ecr"], ["ECR"]), (I["secret"], ["Secrets Manager"]),
                                    (I["kms"], ["KMS"]), (I["s3"], ["S3"]),
                                    (I["ddb"], ["DynamoDB"]), (I["tgw"], ["Transit Gateway"]))):
        c.node(ic, 260 + i * 190, 225, rows, size=52)
    c.lines(1690, 218, ["A regional service has no zone to choose.",
                        "Everything below needs one — and two of them need THREE."],
            size=13, color=MUTE)

    zones = (("A", "dev · uat · prod", Z["app"][0], Z["app"][1], "full"),
             ("B", "uat · prod", Z["app"][0], Z["app"][1], "full"),
             ("C", "prod  ·  plus the quorum services in uat", "#94a3b8", "#f8fafc", "thin"))
    for i, (zid, envs, pen, bg, mode) in enumerate(zones):
        cx = 380 + i * 670
        c.group("AVAILABILITY ZONE  %s" % zid, cx - 320, 380, 640, 1010, stroke=pen,
                fill=bg, sub=envs, label_size=18)

        c.group("inspection VPC  ·  firewall + public  /24", cx - 300, 465, 600, 200,
                stroke=Z["pub"][0], fill=Z["pub"][1], label_size=13, radius=11, width=1.6)
        if mode == "full":
            c.node(I["nfw"], cx - 145, 545, ["Firewall endpoint", "no endpoint = no egress"],
                   size=54)
            c.node(I["nat"], cx + 145, 545, ["NAT + Elastic IP",
                                             "1SB and the PG allowlist it"], size=54)
        else:
            c.ghost(cx, 545, 420, 74, ["firewall endpoint + NAT + EIP — prod only",
                                       "a cost call: each EIP is one more to allowlist"])

        c.group("private-app  /20", cx - 300, 690, 600, 190, stroke=Z["app"][0],
                fill="#ffffff", label_size=13, radius=11, width=1.6)
        tail = "≥ 3 in uat/prod" if mode == "full" else "prod only"
        c.node(I["eks"], cx - 180, 765, ["EKS nodes", tail], size=56)
        c.node(I["alb"], cx, 765, ["Internal ALB", "one node here" if mode == "full" else "prod only"], size=56)
        if mode == "full":
            c.node(I["deploy"], cx + 180, 765, ["sale-path pods", "zone spread + PDB"], size=56)
        else:
            c.ghost(cx + 180, 770, 215, 66, ["sale-path pods", "prod only"])

        c.group("private-data  /24", cx - 300, 905, 600, 450, stroke=Z["dat"][0],
                fill=Z["dat"][1], label_size=13, radius=11, width=1.6)
        # --- relational: one writer, one reader, nothing in the third zone
        if zid == "A":
            c.node(I["aurora"], cx - 145, 985, ["Aurora  WRITER",
                                                "single-AZ by definition"], size=54)
            c.node(I["cache"], cx + 145, 985, ["Valkey  PRIMARY",
                                               "sessions live here"], size=54)
        elif zid == "B":
            c.node(I["aurora"], cx - 145, 985, ["Aurora  READER",
                                                "MUST be a different AZ",
                                                "assert it in IaC"], size=54)
            c.node(I["cache"], cx + 145, 985, ["Valkey  REPLICA",
                                               "automatic failover ON"], size=54)
        else:
            c.ghost(cx - 145, 990, 260, 74, ["Aurora: subnet reserved", "no instance"])
            c.ghost(cx + 145, 990, 260, 74, ["Valkey: 2 AZs is enough", "not a shard"])
        # --- quorum services: the third zone is NOT optional for these
        c.node(I["msk"], cx - 145, 1130, ["MSK broker %d" % (i + 1),
                                          "RF 3 · min.insync 2",
                                          "3 AZs REQUIRED"], size=54)
        if zid == "C":
            c.node(I["srch"], cx + 145, 1130, ["OpenSearch master 3",
                                               "the tie-breaker",
                                               "3 AZs REQUIRED"], size=54)
        else:
            c.node(I["srch"], cx + 145, 1130, ["OpenSearch data + master",
                                               "dedicated masters × 3"], size=54)

    c.lines(1080, 1470, [
        "Two asymmetries matter. The Aurora writer is in ONE zone, and the cache replica pair "
        "needs only two — losing a zone costs a failover, not a service.",
        "The broker and the search masters are QUORUM services: two of three is the difference "
        "between losing a node and losing the cluster, so zone C is not a cost option for them.",
    ], size=14, color=INK)
    return c.save(os.path.join(OUT, "r0-platform-az.svg"))


# =========================================================================
# 3 — DISASTER RECOVERY
# =========================================================================
def dr():
    c = Canvas(2300, 1620, "Disaster recovery — warm standby",
               "RTO ≤ 1 h MEASURED · audit RPO 0 · ap-south-2 (Hyderabad) · "
               "active-active is explicitly not R0")

    c.group("PRIMARY — ap-south-1", 120, 200, 460, 1260, stroke=Z["app"][0],
            fill=Z["app"][1], sub="Mumbai · everything running", label_size=18)
    c.group("DR — ap-south-2", 900, 200, 1300, 1340, stroke=Z["dr"][0], fill=Z["dr"][1],
            sub="Hyderabad", label_size=18)
    c.group("PROVISIONED NOW — in the same change as the primary", 940, 270, 1220, 930,
            stroke="#0f766e", fill="#ccfbf1", label_size=15)
    c.group("NOT RUNNING — created or re-pointed at failover", 940, 1220, 1220, 240,
            stroke="#94a3b8", fill="#ffffff", label_size=15)

    pairs = (
        (I["ecr"], ["ECR"], ["D2  ECR replica"], "continuous"),
        (I["s3"], ["S3 + Object Lock"], ["D3  S3 replica", "Object Lock ON the replica too"], "CRR · RPO 0"),
        (I["aurora"], ["Aurora"], ["D4  Aurora DR", "Global secondary OR backup copy —",
                                   "Aarti decides, and the RTO is MEASURED"], "continuous"),
        (I["ddb"], ["DynamoDB"], ["D5  PITR is mandatory", "global tables = a cost decision"], "PITR"),
        (I["kms"], ["KMS CMKs"], ["D6  KMS replica keys"], "replica"),
        (I["secret"], ["Secrets Manager"], ["D7  Secrets replicas"], "replica"),
        (I["tgw"], ["Transit Gateway", "VPN + Direct Connect"],
         ["D16  TGW + VPN attachment", "a standby that cannot reach CBS",
          "or Bank AD answers nothing"], "provisioned now"),
    )
    for i, (ic, lrows, rrows, tag) in enumerate(pairs):
        y = 355 + i * 122
        a = c.node(ic, 350, y, lrows, size=54)
        b = c.node(ic, 1080, y, rrows, size=54)
        c.link(a.port("R"), b.port("L"), color=REPL, width=2.6, dash="8 5",
               label=tag, label_size=11.5)

    eks_a = c.node(I["eks"], 350, 1320, ["EKS — running"], size=54)
    eks_b = c.node(I["eks"], 1080, 1320, ["D8  EKS", "node groups at 0"], size=54)
    c.link(eks_a.port("R"), eks_b.port("L"), color="#94a3b8", width=2.2, dash="4 5",
           label="NOT replicated", label_size=11.5)
    c.node(I["r53"], 1500, 1320, ["D9  Route 53 failover", "MANUAL in R0"], size=54)
    c.node(I["cf"], 1880, 1320, ["D10  Cloudflare / ALB origin", "re-point — a runbook step"], size=54)

    c.node(I["vpc"], 1500, 370, ["D1  VPC + subnets", "empty · no NAT until failover"], size=54)
    c.node(I["iam"], 1880, 370, ["IAM roles + IaC", "the same modules, a different tfvars"], size=54)

    # --- the three tiers that are deliberately absent, and why
    c.group("DELIBERATELY NOT REPLICATED — reconstructed, not restored", 1330, 500, 790, 300,
            stroke="#94a3b8", fill="#ffffff", label_size=14,
            sub="a tier is replicated when it holds something that cannot be rebuilt")
    c.node(I["cache"], 1470, 610, ["D13  no DR cache", "sessions are", "re-established"], size=50)
    c.node(I["msk"], 1725, 610, ["D14  no Replicator", "events REPLAY from",
                                 "the outbox in Aurora"], size=50)
    c.node(I["srch"], 1980, 610, ["D15  no DR search", "operational logs", "are not evidence"],
           size=50)

    c.group("THE DELIVERABLE IS A RECORD, NOT A DESIGN", 1330, 850, 790, 300,
            stroke=Z["ext"][0], fill="#fef2f2", label_size=14)
    c.node(I["backup"], 1725, 945, [], size=52)
    c.lines(1725, 1010, ["D11  a DR exercise, timed                    (gate S09-G7)",
                         "D12  a rollback drill in UAT               (gate S09-G4)",
                         "NFR-EVT-03  an outbox replay drill  — D14 depends on it",
                         "An untested standby is a claim, not a capability."],
            size=12.5, color=INK)
    return c.save(os.path.join(OUT, "r0-platform-dr.svg"))


# =========================================================================
# 4 — WHEN
# =========================================================================
def sequence():
    bands = (
        ("P0", "GUARDRAILS", "before any resource exists", "#475569", "#f1f5f9",
         ((I["org"], ["6 accounts", "incl. the network account"]),
          (I["trail"], ["security account", "CloudTrail · Config"]),
          (I["kms"], ["CMK hierarchy"]))),
        ("P1", "NETWORK", "START HERE — two external parties", "#ea580c", "#fff7ed",
         ((I["vpc"], ["VPC · 3 AZ subnets"]),
          (I["tgw"], ["TRANSIT GATEWAY", "route table per env"]),
          (I["nfw"], ["inspection VPC", "+ Network Firewall"]),
          (I["nat"], ["NAT + ELASTIC IPs", "publish to 1SB and the PG"]),
          (I["vpn"], ["VPN now, DX ordered", "the bank's own work"]))),
        ("P2", "COMPUTE", "", "#2563eb", "#eff6ff",
         ((I["eks"], ["EKS × 3 environments"]),
          (I["deploy"], ["admission policy", "NetworkPolicy default-deny"]),
          (I["iam"], ["IRSA per deployable"]))),
        ("P3", "DATA + MESSAGING", "", "#475569", "#f8fafc",
         ((I["aurora"], ["Aurora + 16 schemas"]),
          (I["ddb"], ["DynamoDB + PITR"]),
          (I["s3"], ["S3 + OBJECT LOCK", "cannot be applied later"]),
          (I["cache"], ["Valkey", "ACL user per service"]),
          (I["msk"], ["MSK + schema registry", "needed at W1, not W3"]))),
        ("P4", "EDGE + PROXY", "", "#b45309", "#fffaf0",
         ((I["alb"], ["Internal ALB"]),
          (I["apigw"], ["API Gateway", "+ PG callback — needed at W3"]),
          (I["cf"], ["Cloudflare + F5", "External ALB ingress"]))),
        ("P5", "IDENTITY", "WS-2", "#059669", "#f0fdf7",
         ((I["deploy"], ["Keycloak + PDP"]),
          (I["secret"], ["Secrets Manager", "rotation exercised once"]))),
        ("P6", "OBSERVABILITY + SEARCH", "", "#7e22ce", "#faf5ff",
         ((I["amp"], ["AMP + AMG"]),
          (I["cw"], ["CloudWatch", "audit pipe SEPARATE"]),
          (I["srch"], ["OpenSearch + ISM", "P1 and P3 logs land here"]))),
        ("P7", "DELIVERY", "", "#0369a1", "#f2f9ff",
         ((I["ecr"], ["ECR — built once"]),
          (I["argo"], ["Argo CD + GitLab CI"]))),
        ("P8", "PROOF", "GATE-S09 accepts records, not designs", "#16a34a", "#f2fdf5",
         ((I["backup"], ["a restore, TIMED"]),
          (I["trail"], ["a rollback drill"]),
          (I["vpn"], ["DX → VPN failover, timed"]),
          (I["msk"], ["an outbox replay drill"]),
          (I["kms"], ["rotation exercised"]))),
    )
    w, gap, top, bh = 340, 34, 210, 720
    PITCH = 128
    c = Canvas(60 * 2 + len(bands) * w + (len(bands) - 1) * gap, 1090,
               "When — the S09 provisioning sequence",
               "each band is gated on the one before it · P1 first because the Elastic IPs must be "
               "allowlisted by two external parties and the bank must terminate the VPN")
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
    c.text(c.w / 2, 1010, "Nothing in P8 is a design document. Every item is a timed record "
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
