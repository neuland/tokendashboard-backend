# Compose project name of the devcontainer, also its mount target, workspaceFolder and
# display name. Derived from the checkout's path, so a copied .devcontainer/ and justfile
# need no renaming. The parent folder is part of it so that two checkouts with the same
# folder name don't share one compose project and replace each other's containers.
# Passed to the devcontainer CLI only, not exported: COMPOSE_PROJECT_NAME would otherwise
# also redirect `docker compose` in other recipes to this project.
devcontainer_name := lowercase(replace_regex(file_name(parent_directory(justfile_directory())) + "-" + file_name(justfile_directory()), '[^-_a-zA-Z0-9]', ''))
devcontainer := "DOCKER_CLI_HINTS=false COMPOSE_PROJECT_NAME=" + devcontainer_name + " devcontainer"

rebuild-devcontainer:
    @{{devcontainer}} up --remove-existing-container --build-no-cache

# No fixed container name (several checkouts of this repo may each run their own
# devcontainer), so the container is found via the label the devcontainer CLI puts on it.
_up:
    #!/usr/bin/env sh
    set -e
    id=$(docker ps -q --filter "label=devcontainer.local_folder={{justfile_directory()}}")
    if [ "$(printf '%s\n' "$id" | grep -c .)" -gt 1 ]; then
        echo "Several running devcontainers belong to {{justfile_directory()}}; remove the stale one(s) with 'docker rm -f <id>':"
        docker ps --filter "label=devcontainer.local_folder={{justfile_directory()}}" --format '  {{{{.ID}}  {{{{.Names}}  {{{{.Status}}'
        exit 1
    fi
    if [ -z "$id" ]; then
        echo "Starting devcontainer..."
        {{devcontainer}} up
    elif ! docker exec "$id" true >/dev/null 2>&1; then
        echo "Devcontainer reports Up but its mount namespace is stale (Docker daemon likely restarted under it); restarting..."
        docker restart "$id" >/dev/null
        if ! docker exec "$id" true >/dev/null 2>&1; then
            echo "Restart did not clear it; recreating container..."
            {{devcontainer}} up --remove-existing-container
        fi
    fi

# Positional arguments keep every word of the command (and its quoting) intact on its way
# into the container; $1 is HERDR_AGENT, the rest is the command.
[positional-arguments]
_agent $HERDR_AGENT +command: _up
    @shift; {{devcontainer}} exec -- "$@"

# Extra arguments go straight to the agent, e.g. `just claude -c`.
[positional-arguments]
claude *args:
    @{{quote(just_executable())}} --justfile {{quote(justfile())}} _agent claude claude --dangerously-skip-permissions "$@"
