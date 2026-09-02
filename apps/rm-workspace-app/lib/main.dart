/// AU Bank Insurance Distribution Platform — RM Workspace (R0).
///
/// Workstream WS-3. Scope: the R0 RM-assisted Term journey defined in
/// `docs/governance/workstreams/WS-3-PLATFORM-CHARTER.md` §2.2.
///
/// This application runs entirely against an in-memory fake today. It is the
/// interface half of S11 — it is NOT evidence that the S11 vertical slice
/// works, because the bounded contexts behind these interfaces do not exist
/// yet. See `README.md` in this directory for what is real and what is faked.
library;

import 'package:flutter/material.dart';

import 'app.dart';

void main() {
  runApp(const RmWorkspaceApp());
}
