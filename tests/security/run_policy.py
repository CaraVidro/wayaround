from pathlib import Path
import subprocess, tempfile
root=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory() as target:
    sources=[root/'src/main/java/net/caravidro/wayaround/security'/name for name in ('ResourceEvidence.java','TrustHistory.java')]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',target,*map(str,sources),str(root/'tests/security/TrustPolicyTest.java')],check=True)
    subprocess.run(['java','-cp',target,'TrustPolicyTest'],check=True)
