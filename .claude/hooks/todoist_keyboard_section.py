#!/usr/bin/env python3
"""Auto-approve Todoist tool calls that stay inside one section.

The owner's to-do list for this app lives in Todoist: project "Random lists",
section "Keyboard". Calls that only touch that section run without a
permission prompt; everything else (other projects or sections, deleting,
moving a task out of the section, anything this script can't verify) falls
through to the normal prompt.

Used for two hook events (see .claude/settings.json):
- PreToolUse: approve the call if it provably stays in the section.
- PostToolUse: remember the IDs of tasks seen in the section, so completing,
  reopening or editing them by ID can be approved later. The list lives in
  the temp directory, so a new session first lists the section (approved)
  before it can complete or edit tasks without a prompt.
"""
import json
import os
import sys
import tempfile

PROJECT_ID = "6j4pM3j8Hp9PcJpX"  # Random lists
SECTION_ID = "6j5gp662m84WP7G5"  # Keyboard
KNOWN_IDS = os.path.join(tempfile.gettempdir(), "todoist-keyboard-section-task-ids.json")


def load_known():
    try:
        with open(KNOWN_IDS) as f:
            return set(json.load(f))
    except (OSError, ValueError):
        return set()


def save_known(ids):
    try:
        with open(KNOWN_IDS, "w") as f:
            json.dump(sorted(ids), f)
    except OSError:
        pass


def tasks_in(value):
    """Yields every dict with an "id" and "sectionId" found in a tool
    response, also inside text blocks that hold JSON."""
    if isinstance(value, dict):
        if "id" in value and "sectionId" in value:
            yield value
        for v in value.values():
            yield from tasks_in(v)
    elif isinstance(value, list):
        for v in value:
            yield from tasks_in(v)
    elif isinstance(value, str) and value[:1] in "{[":
        try:
            yield from tasks_in(json.loads(value))
        except ValueError:
            pass


def stays_in_section(task, known):
    """For add/update entries: no move to another project, section or parent."""
    return (
        task.get("projectId") in (None, PROJECT_ID)
        and task.get("sectionId") in (None, SECTION_ID)
        and (task.get("parentId") is None or task["parentId"] in known)
    )


def approved(tool, args, known):
    if tool == "find-tasks":
        return args.get("sectionId") == SECTION_ID and not args.get("filterIdOrName")
    if tool == "add-tasks":
        tasks = args.get("tasks") or []
        return bool(tasks) and all(
            t.get("sectionId") == SECTION_ID and stays_in_section(t, known) for t in tasks
        )
    if tool in ("complete-tasks", "uncomplete-tasks"):
        ids = args.get("ids") or []
        return bool(ids) and all(i in known for i in ids)
    if tool == "update-tasks":
        tasks = args.get("tasks") or []
        return bool(tasks) and all(
            t.get("id") in known and stays_in_section(t, known) for t in tasks
        )
    return False


def main():
    try:
        data = json.load(sys.stdin)
    except ValueError:
        return
    tool = data.get("tool_name", "").rsplit("__", 1)[-1]
    event = data.get("hook_event_name")

    if event == "PostToolUse":
        known = load_known()
        added = {t["id"] for t in tasks_in(data.get("tool_response")) if t.get("sectionId") == SECTION_ID}
        if added - known:
            save_known(known | added)
        return

    if approved(tool, data.get("tool_input") or {}, load_known()):
        print(json.dumps({
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "allow",
                "permissionDecisionReason": "Todoist call limited to Random lists > Keyboard",
            }
        }))
    # Otherwise print nothing: the normal permission prompt decides.


if __name__ == "__main__":
    main()
