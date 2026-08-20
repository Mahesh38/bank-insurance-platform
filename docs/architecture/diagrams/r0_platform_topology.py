#!/usr/bin/env python3
"""R0 platform topology — the picture the AWS platform team provisions from.

Four diagrams, deliberately. Each answers one of the questions the platform team
asks, and a single picture cannot carry all four without becoming unreadable:

    r0-platform-topology.png   what runs where — zones, subnets, the proxy chain
    r0-platform-az.png         which availability zone each resource sits in
    r0-platform-dr.png         what exists in ap-south-2, and what does not
    r0-platform-sequence.png   in what order the platform team builds it

Rendering only (rule HA-02): every element is named by R0-LLD.md. If this file and
R0-LLD.md disagree, R0-LLD.md wins and this file is the defect.

Regenerate:  python3 docs/architecture/diagrams/r0_platform_topology.py
Requires:    pip install diagrams   ·   apt-get install graphviz
"""
import os

from diagrams import Cluster, Diagram, Edge, Node
from diagrams.aws.compute import ECR, EKS
from diagrams.aws.database import Aurora, Dynamodb
from diagrams.aws.general import Users
from diagrams.aws.management import (AmazonManagedPrometheus, Cloudtrail,
                                     Cloudwatch, Organizations)
from diagrams.aws.network import (ALB, APIGateway, CloudFront, ELB,
                                  InternetGateway, NATGateway, PrivateSubnet,
                                  PublicSubnet, Route53, VPC)
from diagrams.aws.security import KMS, WAF, IAMRole, SecretsManager
from diagrams.aws.storage import Backup, S3
from diagrams.generic.device import Mobile, Tablet
from diagrams.k8s.compute import Cronjob, Deploy, Pod
from diagrams.onprem.gitops import ArgoCD
from diagrams.onprem.network import Internet
from diagrams.programming.framework import Flutter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = lambda name: os.path.join(HERE, "..", name)

BASE_GRAPH = {
    "fontsize": "30",
    "fontname": "Helvetica-Bold",
    "bgcolor": "white",
    "pad": "0.7",
    "nodesep": "0.9",
    "ranksep": "1.1",
    "splines": "spline",
}
NODE = {"fontsize": "13", "fontname": "Helvetica", "margin": "0.1"}
EDGE = {"fontsize": "12", "fontname": "Helvetica-Bold", "color": "#334155"}


def cl(label, bg, pen, fs="16"):
    """Cluster kwargs — diagrams styles clusters through graph_attr."""
    return {"label": label, "graph_attr": {
        "bgcolor": bg, "pencolor": pen, "style": "rounded", "fontsize": fs,
        "fontname": "Helvetica-Bold", "fontcolor": pen, "penwidth": "2.0",
        "margin": "22"}}


# trust-zone palette, identical to r0-lld.svg so the two read across
Z_DEV = ("#faf5ff", "#7e22ce")
Z_EDGE = ("#fff8ec", "#b45309")
Z_VPC = ("#f6faff", "#1e40af")
Z_PUB = ("#fff7ed", "#ea580c")
Z_APP = ("#eff6ff", "#2563eb")
Z_DAT = ("#f8fafc", "#475569")
Z_MGD = ("#f8fafc", "#64748b")
Z_EXT = ("#fffafa", "#dc2626")
Z_DR = ("#f0fdfa", "#0d9488")

REQ = Edge(color="#1d4ed8", penwidth="3.0")
MONEY = Edge(color="#dc2626", penwidth="3.0")
EGRESS = Edge(color="#b45309", penwidth="2.6", style="dashed")
STATE = Edge(color="#475569", penwidth="2.0", style="dotted")
REPL = Edge(color="#0d9488", penwidth="2.6", style="dashed")


# =========================================================================
# 1 — WHAT RUNS WHERE
# =========================================================================
def topology():
    g = dict(BASE_GRAPH)
    g.update({"nodesep": "1.0", "ranksep": "1.0"})
    with Diagram("R0 on AWS  —  what runs where     ap-south-1 (Mumbai)  ·  one VPC per environment",
                 filename=OUT("r0-platform-topology"), outformat="png", show=False,
                 direction="TB", graph_attr=g, node_attr=NODE, edge_attr=EDGE):

        with Cluster(**cl("DEVICES  —  outside the VPC", *Z_DEV)):
            rm = Flutter("RM\nFlutter app")
            ipr = Tablet("Partner rep\nbrowser")

        with Cluster(**cl("PUBLIC AWS EDGE  —  the only internet entry point", *Z_EDGE)):
            cdn = CloudFront("CloudFront")
            waf = WAF("WAF + Shield")
            agw = APIGateway("API Gateway\nPROXY 1 of 2")
            pgcb = APIGateway("PG callback route\nSEPARATE · IP-allowlisted\nsee the payment diagram")

        with Cluster(**cl("VPC  ·  3 Availability Zones  ·  10.{env}.0.0/16", *Z_VPC, fs="19")):

            with Cluster(**cl("PRIVATE-APP SUBNETS  ·  Amazon EKS  ·  no PVC", *Z_APP, fs="17")):
                alb = ALB("Internal ALB\nPROXY 2 of 2")

                with Cluster(**cl("ns: edge", "#f5f3ff", "#7e22ce", fs="14")):
                    bff = Pod("#2 RM BFF")

                with Cluster(**cl("ns: identity  ·  WS-2", "#ecfdf5", "#059669", fs="14")):
                    idp = Deploy("IdP adapter")
                    pdp = Deploy("PDP\nfails closed")
                    kc = Deploy("Keycloak")

                with Cluster(**cl("ns: shared-platform", "#f0f9ff", "#0369a1", fs="14")):
                    with Cluster(**cl("W0b + W1  ·  journey spine", "#e0f2fe", "#0284c7", fs="13")):
                        cfg = Pod("#19 Config")
                        opp = Pod("#5 Opportunity")
                        jrn = Pod("#9 Journey")
                        cus = Pod("#4 Customer")
                        cat = Pod("#8 Catalogue")
                    with Cluster(**cl("W2 - W4  ·  gate, money, evidence", "#fef3c7", "#d97706", fs="13")):
                        con = Pod("#6 Consent")
                        sui = Pod("#7 Suitability")
                        pay = Pod("#12 Payment")
                        pol = Pod("#13 Policy")
                        aud = Pod("#16 Audit")
                        ntf = Pod("#17 Notification")

                with Cluster(**cl("ns: life-cell  ·  LOB-OWNED", "#fff1f2", "#e11d48", fs="14")):
                    quo = Pod("#10 Quotation")
                    prp = Pod("#11 Proposal")

                with Cluster(**cl("ns: integration", "#f1f5f9", "#475569", fs="14")):
                    hub = Pod("#14 Hub")
                    sb1 = Pod("#15 1SB adapter")

                with Cluster(**cl("ns: jobs", "#f0fdf4", "#16a34a", fs="14")):
                    obx = Deploy("outbox-publisher")
                    crn = Cronjob("reconcile")

            with Cluster(**cl("PUBLIC SUBNETS  —  NAT only, no workloads", *Z_PUB, fs="15")):
                nat = NATGateway("NAT + Elastic IP\nEVERY outbound call leaves here:\n1SB · CBS · PG · SMS\n1SB and the PG allowlist THIS EIP")
                igw = InternetGateway("Internet GW")

            with Cluster(**cl("PRIVATE-DATA SUBNETS  —  no route to the internet", *Z_DAT, fs="15")):
                aur = Aurora("Aurora PostgreSQL\nONE cluster · 16 schemas")

        with Cluster(**cl("AWS-MANAGED  ·  REGIONAL  —  reached over VPC endpoints", *Z_MGD, fs="15")):
            ddb = Dynamodb("DynamoDB\n+ PITR")
            s3 = S3("S3 + Object Lock\n7-year WORM")
            kms = KMS("KMS")
            sec = SecretsManager("Secrets Mgr")
            ecr = ECR("ECR")
            obs = Cloudwatch("CloudWatch\nAMP · AMG · X-Ray")

        with Cluster(**cl("OUTSIDE  —  bank systems and providers are two boundaries", *Z_EXT, fs="15")):
            cbs = Internet("Core Banking\nCBS / CIF")
            ad = Internet("Bank AD")
            pg = Internet("AU Bank\nPayment Gateway")
            onesb = Internet("1SilverBullet\nR0 polls")

        # Layout control. Graphviz puts sibling clusters side by side, which turns
        # six namespaces into a 7000px-wide strip. These invisible edges impose a
        # reading order and wrap the cluster into a block. They carry no meaning.
        INVIS = Edge(style="invis")
        bff - INVIS - idp
        pdp - INVIS - cfg
        cfg - INVIS - con
        con - INVIS - quo
        quo - INVIS - hub
        hub - INVIS - obx

        # the request path — one spine, nothing else
        rm >> REQ >> cdn
        ipr >> REQ >> cdn
        cdn >> REQ >> waf >> REQ >> agw >> REQ >> alb >> REQ >> bff
        bff >> Edge(color="#059669", penwidth="2.4", label="  every call") >> pdp

        # durable state
        jrn >> STATE >> aur
        jrn >> STATE >> ddb
        sb1 >> STATE >> s3

        # egress — the only way out
        hub >> Edge(color="#0369a1", penwidth="2.4", label="  the only way out") >> sb1
        sb1 >> EGRESS >> nat
        nat >> EGRESS >> igw
        igw >> EGRESS >> onesb
        igw >> EGRESS >> cbs

        # The C4 payment path is deliberately NOT drawn here. It runs device -> PG ->
        # callback -> back into the VPC, and drawing that loop on top of the placement
        # picture makes both unreadable. It has its own diagram: r0-platform-payment.png
        pgcb >> Edge(color="#dc2626", penwidth="2.6", style="dashed",
                     label="  C4 — see the payment diagram") >> alb


# =========================================================================
# 2 — WHICH AVAILABILITY ZONE
# =========================================================================
def az():
    g = dict(BASE_GRAPH)
    g.update({"nodesep": "0.7", "ranksep": "0.9"})
    with Diagram("Which availability zone     ap-south-1  ·  pin AZ IDs (aps1-azN), never AZ names",
                 filename=OUT("r0-platform-az"), outformat="png", show=False,
                 direction="TB", graph_attr=g, node_attr=NODE, edge_attr=EDGE):

        with Cluster(**cl("SHARED ACROSS ALL THREE AZs  —  regional, nothing to place", *Z_MGD, fs="15")):
            Dynamodb("DynamoDB")
            S3("S3")
            KMS("KMS")
            SecretsManager("Secrets Manager")
            ECR("ECR")

        def ghost(label):
            return Node(label=label, shape="box", style="dashed,rounded",
                        color="#94a3b8", fontcolor="#94a3b8",
                        fontname="Helvetica", fontsize="13", margin="0.45,0.40")

        anchors = []
        for zid, envs, pen, bg in (
                ("A", "dev · uat · prod", "#1e40af", "#eff6ff"),
                ("B", "uat · prod", "#1e40af", "#eff6ff"),
                ("C", "prod only  (uat: subnets, no paid capacity)", "#64748b", "#f8fafc")):
            with Cluster(**cl("AVAILABILITY ZONE  %s\n%s" % (zid, envs), bg, pen, fs="17")):
                with Cluster(**cl("public /24", *Z_PUB, fs="13")):
                    n = (NATGateway("NAT + EIP") if zid in "AB"
                         else ghost("NAT + EIP\nprod only — a cost call"))
                with Cluster(**cl("private-app /20", *Z_APP, fs="13")):
                    if zid == "C":
                        EKS("EKS nodes\nprod only")
                        ELB("Internal ALB node")
                        ghost("sale-path pods\nprod only")
                    else:
                        EKS("EKS nodes\n>= 3 in uat/prod")
                        ELB("Internal ALB node")
                        Deploy("sale-path pods\nzone spread + PDB")
                with Cluster(**cl("private-data /24", *Z_DAT, fs="13")):
                    if zid == "A":
                        Aurora("Aurora WRITER\nsingle-AZ by definition")
                    elif zid == "B":
                        Aurora("Aurora READER\nMUST be a different AZ\nfrom the writer — assert in IaC")
                    else:
                        ghost("subnet reserved\nno instance")
                anchors.append(n)
        anchors[0] - Edge(style="invis") - anchors[1] - Edge(style="invis") - anchors[2]


# =========================================================================
# 3 — DISASTER RECOVERY
# =========================================================================
def dr():
    g = dict(BASE_GRAPH)
    g.update({"nodesep": "0.8", "ranksep": "1.4"})
    with Diagram("Disaster recovery  —  warm standby     RTO <= 1 h MEASURED  ·  audit RPO 0  ·  active-active is not R0",
                 filename=OUT("r0-platform-dr"), outformat="png", show=False,
                 direction="LR", graph_attr=g, node_attr=NODE, edge_attr=EDGE):

        with Cluster(**cl("PRIMARY  —  ap-south-1 (Mumbai)", "#eff6ff", "#1e40af", fs="18")):
            p_ecr = ECR("ECR")
            p_s3 = S3("S3 + Object Lock")
            p_aur = Aurora("Aurora")
            p_ddb = Dynamodb("DynamoDB")
            p_kms = KMS("KMS CMKs")
            p_sec = SecretsManager("Secrets Manager")
            p_eks = EKS("EKS — running")

        with Cluster(**cl("DR  —  ap-south-2 (Hyderabad)", *Z_DR, fs="18")):
            with Cluster(**cl("PROVISIONED NOW  —  in the same change as the primary", "#ccfbf1", "#0f766e", fs="14")):
                d_vpc = VPC("D1  VPC + subnets\nempty · no NAT yet")
                d_ecr = ECR("D2  ECR replica")
                d_s3 = S3("D3  S3 replica\nObject Lock ON the replica")
                d_kms = KMS("D6  KMS replica keys")
                d_sec = SecretsManager("D7  Secrets replicas")
                d_aur = Aurora("D4  Aurora DR\nGlobal secondary OR Backup copy\nAarti decides — RTO must be MEASURED")
                d_ddb = Dynamodb("D5  PITR mandatory\nglobal tables = a cost decision")
            with Cluster(**cl("NOT RUNNING  —  created or re-pointed at failover", "#ffffff", "#94a3b8", fs="14")):
                d_eks = EKS("D8  EKS\nnode groups at 0")
                d_r53 = Route53("D9  Route 53 failover\nMANUAL in R0")
                d_edge = CloudFront("D10  origin re-point\na runbook step")
            with Cluster(**cl("THE DELIVERABLE IS A RECORD, NOT A DESIGN", "#fef2f2", "#dc2626", fs="14")):
                proof = Backup("D11  DR exercise, timed  (S09-G7)\nD12  rollback drill in UAT  (S09-G4)")

        p_ecr >> REPL >> d_ecr
        p_s3 >> Edge(color="#0d9488", penwidth="3.0", label="  CRR · RPO 0") >> d_s3
        p_aur >> REPL >> d_aur
        p_ddb >> REPL >> d_ddb
        p_kms >> REPL >> d_kms
        p_sec >> REPL >> d_sec
        p_eks >> Edge(color="#94a3b8", style="dashed", label="  NOT replicated") >> d_eks


# =========================================================================
# 4 — WHEN
# =========================================================================
def sequence():
    g = dict(BASE_GRAPH)
    g.update({"nodesep": "0.5", "ranksep": "1.5", "fontsize": "26"})
    with Diagram("When  —  the S09 provisioning sequence     each band is gated on the one before it",
                 filename=OUT("r0-platform-sequence"), outformat="png", show=False,
                 direction="LR", graph_attr=g, node_attr=NODE, edge_attr=EDGE):

        with Cluster(**cl("P0  GUARDRAILS  —  before any resource exists", "#f1f5f9", "#475569", fs="15")):
            p0 = Organizations("5 accounts\nregion SCP · TF state")
            Cloudtrail("security account")
            KMS("CMK hierarchy")

        with Cluster(**cl("P1  NETWORK  —  START HERE, longest external lead time", "#fff7ed", "#ea580c", fs="15")):
            p1 = VPC("VPC · 3 AZ subnets")
            NATGateway("NAT + ELASTIC IPs\npublish to 1SB + PG")
            PrivateSubnet("VPC endpoints")

        with Cluster(**cl("P2  COMPUTE", "#eff6ff", "#2563eb", fs="15")):
            p2 = EKS("EKS x 3 envs")
            Deploy("admission policy\nNetworkPolicy")

        with Cluster(**cl("P3  DATA", "#f8fafc", "#475569", fs="15")):
            p3 = Aurora("Aurora + schemas")
            Dynamodb("DynamoDB + PITR")
            S3("S3 + OBJECT LOCK\ncannot be applied later")

        with Cluster(**cl("P4  EDGE + PROXY", "#fff8ec", "#b45309", fs="15")):
            p4 = ALB("Internal ALB")
            APIGateway("API Gateway\n+ PG callback — needed at W3")
            CloudFront("CloudFront + WAF")

        with Cluster(**cl("P5  IDENTITY (WS-2)", "#ecfdf5", "#059669", fs="15")):
            p5 = Deploy("Keycloak + PDP")
            IAMRole("IRSA per deployable")

        with Cluster(**cl("P6  OBSERVABILITY", "#f5f3ff", "#7e22ce", fs="15")):
            p6 = AmazonManagedPrometheus("AMP + AMG")
            Cloudwatch("CloudWatch\naudit pipe SEPARATE")

        with Cluster(**cl("P7  DELIVERY", "#f0f9ff", "#0369a1", fs="15")):
            p7 = ECR("ECR · built once")
            ArgoCD("Argo CD")

        with Cluster(**cl("P8  PROOF  —  GATE-S09 accepts records, not designs", "#f0fdf4", "#16a34a", fs="15")):
            p8 = Backup("restore, TIMED\nrollback drill\nrotation exercised")

        gate = Edge(color="#1d4ed8", penwidth="3.0")
        p0 >> gate >> p1 >> gate >> p2 >> gate >> p3 >> gate >> p4
        p4 >> gate >> p5 >> gate >> p6 >> gate >> p7 >> gate >> p8


# =========================================================================
# 5 — THE C4 PAYMENT PATH
# =========================================================================
def payment():
    g = dict(BASE_GRAPH)
    g.update({"nodesep": "0.8", "ranksep": "1.6", "fontsize": "26"})
    with Diagram("The C4 payment path  —  the hop people get wrong     "
                 "money is recovered by reconciliation, never by a database restore",
                 filename=OUT("r0-platform-payment"), outformat="png", show=False,
                 direction="LR", graph_attr=g, node_attr=NODE, edge_attr=EDGE):

        with Cluster(**cl("OUTSIDE OUR VPC", *Z_DEV, fs="16")):
            cust = Mobile("Customer device\nnot an RM device — FF-14")

        with Cluster(**cl("AU BANK  —  outside the platform boundary", *Z_EXT, fs="16")):
            pg = Internet("AU Bank Payment Gateway\nhosted page · 3-D Secure")
            settle = Internet("settlement file\narrives out-of-band")

        with Cluster(**cl("OUR EDGE  —  a SEPARATE route from RM traffic (TB-6)", *Z_EDGE, fs="16")):
            pgcb = APIGateway("PG-callback route\nIP-allowlisted to the PG only\nnot on the RM session path")

        with Cluster(**cl("INSIDE THE VPC", *Z_APP, fs="16")):
            alb = ALB("Internal ALB")
            pay = Pod("#12 Payment\nverifies the PG signature")
            recon = Cronjob("payment-reconcile\nS-15 · never auto-resolves a break")
            pol = Pod("#13 Policy\nissues iff RECONCILED")

        with Cluster(**cl("EVIDENCE", *Z_DAT, fs="16")):
            aur = Aurora("payment schema")
            s3 = S3("raw payload\n7-year WORM")

        pay >> Edge(color="#dc2626", penwidth="3.0", label="  1  pay-link to the CUSTOMER device") >> cust
        cust >> Edge(color="#dc2626", penwidth="3.0", label="  2  the RM never sees this URL") >> pg
        pg >> Edge(color="#dc2626", penwidth="3.0", label="  3  callback") >> pgcb
        pgcb >> Edge(color="#dc2626", penwidth="3.0") >> alb >> Edge(color="#dc2626", penwidth="3.0") >> pay
        settle >> Edge(color="#b45309", penwidth="2.4", style="dashed", label="  4  settlement") >> recon
        recon >> Edge(color="#16a34a", penwidth="2.6", label="  5  RECONCILED") >> pol
        pay >> STATE >> aur
        pay >> STATE >> s3


if __name__ == "__main__":
    topology()
    az()
    dr()
    sequence()
    payment()
    print("wrote r0-platform-topology / -az / -dr / -sequence / -payment .png")
