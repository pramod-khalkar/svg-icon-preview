# SVG Toolkit — Incremental & Easy Roadmap

Goal: evolve "SVG Icon Preview" (v1.0.0) into a developer-friendly SVG Toolkit using many small, understandable releases. Each release is intentionally small so you get value quickly and can iterate.

Principles
----------
- Small, focused releases (weeks, not months)
- Clear acceptance criteria per release
- Backwards-compatible where possible
- Measure & iterate based on user feedback

Current baseline
----------------
- v1.0.0 (current)
  - Gutter previews for base64 SVG data URIs
  - Popup preview with zoom
  - Marketplace package and build automation
  - README, sample assets (docs/sample-icons.json)

Micro-release roadmap (short, clear, incremental)
-------------------------------------------------

v1.0.1 — Bugfixes & UX polish (1 week)
- Why: small fixes improve stability and user trust
- Scope:
  - Fix any visible rendering glitches
  - Improve popup positioning and close behavior
  - Add a keyboard shortcut to open the preview
- Acceptance:
  - No major rendering bugs in sample files
  - Popup opens and closes reliably; shortcut works

v1.0.2 — Faster previews & background rendering (1 week)
- Why: reduce UI lag when showing previews
- Scope:
  - Move SVG rendering to a background thread
  - Add a small (loading) placeholder while rendering
- Acceptance:
  - Editor remains responsive when previews load
  - Placeholder visible for slow renders only

v1.0.3 — README examples & sample files (1 week)
- Why: better onboarding for new users
- Scope:
  - Add more sample JSONs and small example projects in /docs
  - Short GIF showing preview workflow in README
- Acceptance:
  - README contains 2 short examples and 1 GIF
  - Users can reproduce preview easily from examples

v1.1.0 — Inspector (small, usable) (2 weeks)
- Why: show key metadata and useful actions without heavy UI
- Scope:
  - Simple Tool Window "Inspector" that lists:
    - Detected SVGs in the current file
    - Basic metadata: viewBox, width, height
    - Actions: Copy Data URI, Copy PNG
- Acceptance:
  - Inspector lists items for a file with data URIs
  - Copy actions place expected content on clipboard

v1.1.1 — Hover details & quick actions (1 week)
- Why: quick access without opening the Inspector
- Scope:
  - Hover popup shows metadata + small action buttons (Copy, Open Inspector)
- Acceptance:
  - Hover shows within 100–200ms and buttons work

v1.2.0 — Local icon library (MVP) (2–3 weeks)
- Why: let users collect and reuse icons
- Scope:
  - Simple library persisted per-project (JSON file in .idea or .svg-library)
  - UI to view thumbnails and insert into editor (copy data URI)
  - Import/export JSON
- Acceptance:
  - Library persists between IDE restarts
  - Import/export works with docs/sample-icons.json

v1.2.1 — Tagging & Search (1 week)
- Why: find icons quickly
- Scope:
  - Add tags and a search box to the library UI
- Acceptance:
  - Search returns matching icons; tags filter correctly

v1.3.0 — Quick editor (edit source + preview) (2–3 weeks)
- Why: enable small edits without leaving IDE
- Scope:
  - Open the SVG source in a small dialog or editor tab with live preview
  - Simple operations: replace color (find/replace on `fill`), scale (set width/height)
- Acceptance:
  - Changes reflect in preview live
  - Save writes changes back to the original place or clipboard

v1.3.1 — Undo/Redo in Quick Editor (1 week)
- Scope:
  - In-memory undo/redo while editing before save
- Acceptance:
  - Undo/redo works for edit actions

v1.4.0 — Export & Optimize (MVP) (2–4 weeks)
- Why: make icons production-ready
- Scope:
  - Export to PNG (choose size) and to optimized SVG (strip comments/metadata)
  - Use a Java optimizer or call `svgo` if available (optional)
- Acceptance:
  - Exports generate expected outputs and optimized size is reduced

v1.4.1 — Batch export (1 week)
- Scope:
  - Export selected library items in one operation
- Acceptance:
  - Batch produces files in chosen output dir

v1.5.0 — Code snippets & templates (2–3 weeks)
- Why: help developers quickly insert components
- Scope:
  - Generate small code snippets: React component (TSX), inline SVG, Android VectorDrawable
  - Simple UI to choose template and copy result
- Acceptance:
  - Generated snippet compiles in minimal sample or pastes correctly

Maintenance releases (ongoing)
-----------------------------
- v1.x.y: patches and small improvements based on user feedback
- Keep `since-build` updated for new IntelliJ releases

How to read this roadmap
------------------------
- Each release is intentionally small and deliverable in 1–3 weeks
- Acceptance criteria are concrete — they help QA and define "done"
- After each minor release, gather feedback and adjust priorities

Next recommended action (pick one)
-----------------------------------
1. Create issues for v1.0.1, v1.0.2 and v1.0.3 (quick wins)
2. Start implementing v1.1.0 Inspector scaffold

Tell me which option you prefer and I will either create GitHub issues and a milestone or scaffold the Inspector tool window in the repo.
