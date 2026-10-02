// Renders ```mermaid fenced blocks. Plain MkDocs emits them as
// <pre><code class="language-mermaid">, which mermaid does not pick up by itself.
import mermaid from "https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs";

for (const code of document.querySelectorAll("pre > code.language-mermaid")) {
  const diagram = document.createElement("pre");
  diagram.className = "mermaid";
  diagram.textContent = code.textContent;
  code.parentElement.replaceWith(diagram);
}

mermaid.initialize({ startOnLoad: false, theme: "neutral" });
await mermaid.run();
