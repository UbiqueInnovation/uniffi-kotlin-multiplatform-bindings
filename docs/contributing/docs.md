# Writing docs

The documentation is built with [MkDocs](https://www.mkdocs.org) and its built-in `readthedocs`
theme. The sources are Markdown files in `docs/`, and the navigation is defined in `mkdocs.yml`.

## Building locally

```bash
python3.12 -m venv .venv
.venv/bin/pip install -r docs/requirements.txt
.venv/bin/mkdocs serve          # http://127.0.0.1:8000, reloads on change
.venv/bin/mkdocs build --strict # what CI should run: fails on broken links
```

Use Python 3.12 or 3.13. On 3.14, MkDocs' file watcher (`watchdog`) currently has no prebuilt
wheel for macOS and fails to build.

`site/` and `.venv/` are ignored by git.

## Conventions

- **Two audiences.** `guide/` is for users of the plugin, `internals/` for people changing it. Don't
  put implementation detail in the guide. Link to the internals page instead.
- **Show code.** Each feature page shows the Rust side, the Kotlin you get, and the configuration.
  Take examples from `examples/` and `tests/uniffi/` so they stay true, and link to the fixture.
- **Describe `main`.** Feature-branch work goes into the docs when it is merged.
- **Links to files in the repository** use full GitHub URLs. Relative links only work between
  pages in `docs/`, and `--strict` treats anything else as an error.
- **Diagrams** are Mermaid, in a fenced code block with the language `mermaid`. `docs/js/mermaid-init.js` renders
  them in the browser. GitHub renders them too when viewing the Markdown file.
- **Admonitions** use the `!!! note` / `!!! warning` syntax.
- New pages must be added to `nav` in `mkdocs.yml`.

## Publishing

The docs aren't published yet. They are meant to go to GitHub Pages at
`https://ubiqueinnovation.github.io/uniffi-kotlin-multiplatform-bindings/` (already set as
`site_url`). Either of these works without changes to the docs:

- `mkdocs gh-deploy` pushes the built site to a `gh-pages` branch, or
- a GitHub Actions workflow that runs `mkdocs build --strict`, then
  `actions/upload-pages-artifact` and `actions/deploy-pages`.
