import net.caravidro.wayaround.security.*;
public final class TrustPolicyTest {
    private static int checks;
    private static final ResourceEvidence STRONG=new ResourceEvidence(255,0,15,ResourceEvidence.ALL_MASK,"a".repeat(64));
    private static final ResourceEvidence CLEAN=new ResourceEvidence(0,0,15,ResourceEvidence.ALL_MASK,"b".repeat(64));
    private static void check(boolean pass,String message) { checks++;if(!pass)throw new AssertionError(message); }
    private static TrustHistory.Action sample(TrustHistory h,double seconds) { return h.observe(STRONG,seconds,false,10,30,60); }
    public static void main(String[] args) {
        check(STRONG.strong(),"Broad host transparency with intact ores is strong");
        check(!new ResourceEvidence(31,0,15,ResourceEvidence.ALL_MASK,"a".repeat(64)).strong(),"Five altered textures cannot convict");
        check(!new ResourceEvidence(255,0,1,ResourceEvidence.ALL_MASK,"a".repeat(64)).strong(),"One ore control is insufficient");
        check(new ResourceEvidence(0,255,15,ResourceEvidence.ALL_MASK,"a".repeat(64)).strong(),"Empty terrain models detected");
        check(!new ResourceEvidence(-1,0,15,ResourceEvidence.ALL_MASK,"a".repeat(64)).valid(),"Negative/unknown mask bits rejected");
        check(!new ResourceEvidence(255,0,15,0,"a".repeat(64)).valid(),"Cannot claim unchecked textures");
        check(!new ResourceEvidence(255,0,15,ResourceEvidence.ALL_MASK,"pack-name").valid(),"Pack name cannot spoof fingerprint");
        check(!ResourceEvidence.transparentSample(100,256),"Normal alpha remains below evidence threshold");
        check(ResourceEvidence.transparentSample(200,256),"Severely transparent texture sampled");
        check(CLEAN.clean(),"Full clean observation");
        TrustHistory h=new TrustHistory();
        check(sample(h,5)==TrustHistory.Action.NONE,"Five seconds no verdict");
        check(sample(h,5)==TrustHistory.Action.PRIVATE_WARNING,"Ten seconds retained with owner/player warning");
        double kept=h.evidenceSeconds;
        for(int i=0;i<100;i++)check(h.observe(CLEAN,5,false,10,30,60)==TrustHistory.Action.NONE,"Removing pack stops all sanctions");
        check(h.evidenceSeconds==kept,"Removing pack does not erase proof");
        for(int i=0;i<3;i++)check(sample(h,5)==TrustHistory.Action.NONE,"Private grace accumulates active use only");
        check(sample(h,5)==TrustHistory.Action.PUBLIC_WARNING,"Continued use broadcasts at 30 seconds");
        for(int i=0;i<5;i++)check(sample(h,5)==TrustHistory.Action.NONE,"No early kick");
        check(sample(h,5)==TrustHistory.Action.KICK,"Sixty active seconds produces kick");
        check(h.expelled()==TrustHistory.Action.KICK && h.kicks==1,"Actual first expulsion counted");
        for(int round=2;round<=3;round++) {
            check(sample(h,5)==TrustHistory.Action.PRIVATE_WARNING,"Returning user receives private warning");
            for(int i=0;i<4;i++)check(sample(h,5)==TrustHistory.Action.NONE,"Fresh grace per expulsion round");
            check(sample(h,5)==TrustHistory.Action.PUBLIC_WARNING,"Repeat public warning");
            for(int i=0;i<5;i++)check(sample(h,5)==TrustHistory.Action.NONE,"Rejoin cannot shorten 60 seconds");
            check(sample(h,5)==TrustHistory.Action.KICK,"Returning cheater observed for one minute");
            check(h.expelled()==(round==3?TrustHistory.Action.BAN:TrustHistory.Action.KICK),"Three rounds permanently ban");
        }
        check(h.banned && h.kicks==3,"Permanent escalation");h.pardoned();
        check(!h.banned && h.kicks==0 && h.lifetimeSeconds==180,"Pardon resets enforcement, preserves lifetime proof");
        h=new TrustHistory();sample(h,Double.NaN);sample(h,Double.POSITIVE_INFINITY);sample(h,-1);
        check(h.evidenceSeconds==0,"Invalid clocks never create proof");sample(h,3600);
        check(h.evidenceSeconds==5,"Lag/offline interval capped to one sample");
        TrustHistory approved=new TrustHistory();for(int i=0;i<1000;i++)approved.observe(STRONG,5,true,10,30,60);
        check(approved.evidenceSeconds==0,"Approved pack does not accrue evidence");
        // High preexisting evidence cannot skip freshly delivered warning grace (audit-mode transition).
        h=new TrustHistory();h.evidenceSeconds=500;h.roundSeconds=500;
        check(sample(h,5)==TrustHistory.Action.PRIVATE_WARNING,"New enforcement always privately warns first");
        for(int i=0;i<3;i++)check(sample(h,5)==TrustHistory.Action.NONE,"Existing score cannot skip private grace");
        check(sample(h,5)==TrustHistory.Action.PUBLIC_WARNING,"Twenty new seconds needed after private warning");
        for(int i=0;i<3;i++)check(sample(h,5)==TrustHistory.Action.NONE,"Existing score cannot skip public grace");
        check(sample(h,5)==TrustHistory.Action.KICK,"Twenty new seconds needed after public warning");
        // Exhaustive legal mask combinations verify overlap does not double-count distinct terrain types.
        for(int mask=0;mask<=ResourceEvidence.HOST_MASK;mask++) {
            ResourceEvidence e=new ResourceEvidence(mask,mask,15,ResourceEvidence.ALL_MASK,"a".repeat(64));
            check(e.anomalies()==Integer.bitCount(mask),"Alpha/model overlap is one host");
            check(e.strong()==(Integer.bitCount(mask)>=6),"Distinct-host threshold exact");
        }
        System.out.println("Anti-Xray policy: "+checks+" assertions passed (false positives, toggle/rejoin, grace, 3 strikes, approval, masks)");
    }
}
