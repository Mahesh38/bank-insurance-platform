# Loading Protocol

Use this order for humans, agents and retrieval systems:

1. Load `context-manifest.yaml`.
2. Resolve the requested loading profile.
3. Load the project problem statement before domain or role interpretation.
4. Load canonical authority and binding sources before asserting a decision right.
5. Load only the roles and protocols materially affected by the decision.
6. Attach source paths and freshness/ratification status to consequential conclusions.
7. When sources conflict, apply manifest precedence and report the conflict.

## Minimum output contract

A consequential context-assisted decision states:

```text
project · problem/outcome · current facts · assumptions · affected authority
options · recommendation · evidence · unresolved owner/date · next safe action
```

Context may recommend, explain, draft and assemble evidence. It does not manufacture a human
signature, legal interpretation, production authority or material risk acceptance.

## Retrieval guidance

- Index framework documents once and project overlays separately.
- Tag chunks with `project_id`, `layer_id`, `role_id`, `governance_status` and source path.
- Prefer a narrow loading profile over retrieving every persona.
- Exclude compatibility redirects from embeddings; index their canonical target instead.
