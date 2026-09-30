package net.caravidro.wayaround.industrial.crushing;

public final class CrushingRulesTest {
    public static void main(String[] args) {
        for(var size:CrusherSize.values())for(int multiplier:new int[]{1,2})
            for(int count:new int[]{1,8,32,64})for(int room:new int[]{0,1,8,64,128,512})for(int yield:new int[]{1,9}) {
                int admitted=size.boundedBatch(count,room,yield,multiplier);
                require(admitted>=0 && admitted<=count && admitted*yield<=room,"Batch must conserve input and respect output budget");
                if(size==CrusherSize.SMALL)require(admitted<=1,"Small crusher must never acquire bulk capacity from a wide hopper");
            }
        require(CrusherSize.LARGE.boundedBatch(64,512,9,2)==56,"Raw-metal blocks must process a partial batch rather than deadlock on 576 output");
        require(CrusherSize.MEDIUM.boundedBatch(64,512,1,1)==8,"Medium has a real parallel batch");
        require(CrusherSize.LARGE.inputCapacity(2)>CrusherSize.MEDIUM.inputCapacity(2),"Large supports bounded bulk storage");
        for(var size:CrusherSize.values()) {
            var light=MachinePartSpec.tool(size,false);var reinforced=MachinePartSpec.tool(size,true);
            require(reinforced.strength()>light.strength() && reinforced.driveCost()>light.driveCost()
                && reinforced.speed()<light.speed(),"Reinforcement must trade strength for power and throughput");
            for(var other:CrusherSize.values())require(light.fits(other.id())==(size==other),"Scale-specific tooling cannot crossfit");
            require(!light.fits("mill"),"Crushing tools must not become millstones");
        }
        require(MachinePartSpec.LIGHT_SHAFT.speed()>MachinePartSpec.REINFORCED_SHAFT.speed()
            && MachinePartSpec.LIGHT_SHAFT.strength()<MachinePartSpec.REINFORCED_SHAFT.strength(),"Drive variants must have a tradeoff");
        require(MachinePartSpec.COPPER_BEARING.driveCost()<MachinePartSpec.PLAIN_BEARING.driveCost()
            && MachinePartSpec.COPPER_BEARING.abrasion()>MachinePartSpec.PLAIN_BEARING.abrasion(),"Low-friction bearing sacrifices wear resistance");
        require(MachinePartSpec.WIDE_FEED.feedMultiplier()>MachinePartSpec.NARROW_FEED.feedMultiplier()
            && MachinePartSpec.WIDE_FEED.driveCost()>MachinePartSpec.NARROW_FEED.driveCost(),"Bulk intake requires more power");
        require(MachinePartSpec.STONE_MILL.fits("mill") && !MachinePartSpec.STONE_MILL.fits("small"),"Shared system must preserve operation-specific tool roles");
        System.out.println("Crushing conservation, scale and component tradeoff regressions passed");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
