"""pre-commit 용 Gradle 래퍼 런처 (크로스 플랫폼).

윈도우는 gradlew.bat, 그 외(macOS/Linux)는 ./gradlew 를 호출한다.
pre-commit 이 직접 ./gradlew(셰뱅 #!/bin/sh)를 exec 하면 윈도우에서
'/bin/sh not found' 로 실패하므로, OS 를 판별해 올바른 래퍼를 부른다.

사용: python config/hooks/gradlew_task.py <task...>
"""

import os
import pathlib
import subprocess
import sys

# 이 스크립트는 <root>/config/hooks/ 에 있으므로 상위 2단계가 프로젝트 루트.
root = pathlib.Path(__file__).resolve().parents[2]
tasks = sys.argv[1:]

if os.name == "nt":
    cmd = ["cmd", "/c", str(root / "gradlew.bat"), *tasks]
else:
    cmd = [str(root / "gradlew"), *tasks]

sys.exit(subprocess.call(cmd, cwd=str(root)))
