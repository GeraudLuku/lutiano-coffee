"""Proves the refactored API behaves like the original.

    ORACLE_JAR=/path/to/lutiano-api-baseline.jar python3 compare.py [--no-flows]

Starts the ORIGINAL build (the jar made before the refactor) on port 8091 against the same database, snapshots it and the
current API (port 8080) with the same tokens at the same database state, and diffs them: GET responses by value, write flows
and error cases by shape. Any difference is printed and the exit code is 1.
"""
import os
import subprocess
import sys
import time
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
JAR = os.environ.get('ORACLE_JAR')
ORACLE_PORT = 8091


def run(cmd, env_base):
    env = dict(os.environ, LUTIANO_BASE=env_base)
    subprocess.run(cmd, cwd=HERE, env=env, check=True)


def wait_health(port, seconds=150):
    for _ in range(seconds // 2):
        try:
            if urllib.request.urlopen(f'http://localhost:{port}/api/v1/health', timeout=3).status == 200:
                return True
        except Exception:
            time.sleep(2)
    return False


def main():
    if not JAR:
        sys.exit('Set ORACLE_JAR to the jar built before the refactor.')
    flows = '--no-flows' not in sys.argv
    env = dict(os.environ, DB_URL='jdbc:mariadb://localhost:3307/lutiano', REDIS_PORT='6380', SEARCH_INDEXING_ENABLED='false')
    oracle = subprocess.Popen(['java', '-jar', JAR, f'--server.port={ORACLE_PORT}'], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        if not wait_health(ORACLE_PORT):
            sys.exit('The original build did not start on port %d.' % ORACLE_PORT)
        old, new = f'http://localhost:{ORACLE_PORT}', 'http://localhost:8080'
        os.makedirs(os.path.join(HERE, 'out'), exist_ok=True)
        status = 0
        steps = [('snapshot.py', 'get')] + ([('flows.py', 'flows')] if flows else [])
        for script, name in steps:
            run([sys.executable, script, f'out/{name}_old.json'], old)
            run([sys.executable, script, f'out/{name}_new.json'], new)
            print(f'--- {name}: original vs refactored')
            status |= subprocess.run([sys.executable, 'diff.py', f'out/{name}_old.json', f'out/{name}_new.json'], cwd=HERE).returncode
        sys.exit(status)
    finally:
        oracle.terminate()


if __name__ == '__main__':
    main()
