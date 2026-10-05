import subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory() as directory:
    files=[
        root/'src/main/java/net/caravidro/wayaround/worldgen/planet/PlanetMath.java',
        root/'src/main/java/net/caravidro/wayaround/ecology/AnimalClimateProfile.java',
        root/'tests/geography/PlanetMathTest.java',
        root/'tests/geography/AnimalClimateProfileTest.java',
    ]
    subprocess.run(['java','com.sun.tools.javac.Main','-d',directory,*map(str,files)],check=True)
    subprocess.run(['java','-cp',directory,'net.caravidro.wayaround.worldgen.planet.PlanetMathTest'],check=True)
    subprocess.run(['java','-cp',directory,'net.caravidro.wayaround.ecology.AnimalClimateProfileTest'],check=True)
