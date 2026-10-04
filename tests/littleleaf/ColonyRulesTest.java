import net.caravidro.wayaround.littleleaf.ColonyRules;
import net.caravidro.wayaround.littleleaf.world.ColonyLayout;
public final class ColonyRulesTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
        check(ColonyRules.stage(640,true)==4,"Fortress threshold");check(ColonyRules.stage(4096,false)==1,"Unenchanted colony stays small");
        check(ColonyRules.advanceWork(0,1200,true)==1,"Ecological aging");check(ColonyRules.advanceWork(0,999999999,true)==600,"Offline catchup bounded to thirty days");
        check(ColonyRules.advanceWork(10,24000,false)==10,"Unsuitable habitat stops aggregate work");
        for(int i=-5;i<10;i++){check(ColonyRules.population(i,false)<=14,"Surface population cap");check(ColonyRules.radius(i,true)<=12,"Construction radius cap");}
        for(int cell=-4;cell<4;cell++){
            for(int x=24;x<=88;x++)check(ColonyLayout.sample(cell*128+x,32,64)!=ColonyLayout.Material.SOIL,"Connected main route");
            for(int z=32;z<=96;z++)check(ColonyLayout.sample(cell*128+52,32,z)!=ColonyLayout.Material.SOIL,"Connected smaller tunnels");
            check(ColonyLayout.sample(cell*128+6,32,64)==ColonyLayout.Material.EXIT,"Exit is stable across signed cells");
            for(int x=4;x<24;x++)check(ColonyLayout.sample(cell*128+x,33,64)!=ColonyLayout.Material.SOIL,"Walkable escape tunnel");
            check(ColonyLayout.sample(cell*128+24,31,64)==ColonyLayout.Material.SOIL,"Safe supported arrival");
            check(ColonyLayout.sample(cell*128+50,32,64)==ColonyLayout.Material.FUNGUS,"Harvestable fungus culture");
            check(ColonyLayout.sample(cell*128+52,43,64)==ColonyLayout.Material.CAP,"Giant mushroom");
            check(ColonyLayout.sample(cell*128,40,64)==ColonyLayout.Material.SOIL,"Colonies stay isolated");
        }
        int small=0,big=0,different=0;for(int x=4;x<124;x++)for(int z=4;z<124;z++){
            var a=ColonyLayout.sample(x,33,z,0);var b=ColonyLayout.sample(x,33,z,4);if(a==ColonyLayout.Material.AIR)small++;if(b==ColonyLayout.Material.AIR)big++;if(ColonyLayout.sample(x+128,33,z,4)!=b)different++;
        }check(big>small*1.3,"Physical expansion produces larger connected galleries");check(different>100,"Different colony cells have different procedural rooms");
        System.out.println("Little Leaf lifecycle and interior contracts passed");
    }
}
