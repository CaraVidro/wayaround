package net.caravidro.wayaround.dream;
public record DreamPlayerProfile(boolean likesContainers,float jumpFrequency,double movementSpeed,int stareMinTicks,int stareMaxTicks) {
    public static DreamPlayerProfile from(PlayerBehaviorProfile profile){
        double sprint=Math.clamp((double)profile.sprintSeconds/Math.max(1,profile.seconds),0,1);
        return new DreamPlayerProfile(profile.containers>0,
                (float)Math.clamp(profile.jumpsPerMinute()/60.0,.01,.70),.85+sprint*.5,14,60);
    }
}
