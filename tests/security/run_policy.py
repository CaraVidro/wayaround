from pathlib import Path
import subprocess, tempfile
root=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory() as target:
    sources=[root/'src/main/java/net/caravidro/wayaround/security'/name for name in ('ResourceEvidence.java','TrustHistory.java','VisibilityMath.java')]
    tests=['TrustPolicyTest','VisibilityMathTest']
    subprocess.run(['java','com.sun.tools.javac.Main','-d',target,*map(str,sources),*[str(root/'tests/security'/f'{test}.java') for test in tests]],check=True)
    for test in tests:subprocess.run(['java','-cp',target,test],check=True)
