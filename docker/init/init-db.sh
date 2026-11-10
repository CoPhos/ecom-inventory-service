#!/bin/bash
set -Eeuo pipefail

# Keep the server and initialization in one supervised process lifecycle.
/opt/mssql/bin/sqlservr &
sqlserver_pid=$!

cleanup() {
  if kill -0 "$sqlserver_pid" 2>/dev/null; then
    kill -TERM "$sqlserver_pid" 2>/dev/null || true
  fi
  wait "$sqlserver_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 143' TERM
trap 'exit 130' INT

export SQLCMDPASSWORD="${MSSQL_SA_PASSWORD:?MSSQL_SA_PASSWORD must be set}"
sqlcmd=(/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b)

echo "Waiting for SQL Server to be ready..."
ready=false
for i in {1..60}; do
  if ! kill -0 "$sqlserver_pid" 2>/dev/null; then
    echo "SQL Server exited before initialization." >&2
    exit 1
  fi

  if "${sqlcmd[@]}" -l 2 -t 5 -Q "SELECT 1" > /dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 2
done

if [ "$ready" != true ]; then
  echo "SQL Server did not become ready in time." >&2
  exit 1
fi

echo "SQL Server is ready. Initializing InventoryServiceDb..."
"${sqlcmd[@]}" -l 5 -t 60 -i /usr/src/app/init-db.sql
echo "Inventory database initialization completed."

wait "$sqlserver_pid"
