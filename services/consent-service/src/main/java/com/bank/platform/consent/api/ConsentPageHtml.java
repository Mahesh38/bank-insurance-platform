package com.bank.platform.consent.api;

/** Customer page for BRD 9.10. The token stays in the URL and is not repeated in the body. */
final class ConsentPageHtml {

  private ConsentPageHtml() {}

  static String render(String kind, String statement) {
    String body =
        switch (kind) {
          case "OPEN" -> open(statement);
          case "AGREED" ->
              "<h1>Consent recorded</h1><p>Your consent has been recorded. You can close this page.</p>";
          case "DISAGREED" ->
              "<h1>Consent not given</h1><p>You did not agree to share your personal details. You can close this page.</p>";
          case "EXPIRED" ->
              "<h1>Link expired</h1><p>This link has expired. Ask your advisor to send a new one.</p>";
          case "USED" -> "<h1>Link already used</h1><p>This link has already been used.</p>";
          default -> "<h1>Link not valid</h1><p>This link is not valid.</p>";
        };
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="no-referrer">
        <title>Consent for sharing of personal details</title>
        <style>
          body { font-family: sans-serif; margin: 2rem auto; max-width: 36rem; line-height: 1.45; }
          button { min-height: 2.75rem; margin: 0 0.75rem 0.75rem 0; padding: 0.5rem 1rem; }
        </style>
        </head>
        <body>
        %s
        </body>
        </html>
        """
        .formatted(body);
  }

  private static String open(String statement) {
    String text = escape(statement == null ? "" : statement);
    return """
        <h1>Consent for sharing of personal details</h1>
        <p>%s</p>
        <form method="post">
          <input type="hidden" name="decision" value="AGREE">
          <button type="submit">Agree</button>
        </form>
        <form method="post">
          <input type="hidden" name="decision" value="DISAGREE">
          <button type="submit">Disagree</button>
        </form>
        """
        .formatted(text);
  }

  static String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
