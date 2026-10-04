package net.caravidro.wayaround.security;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** UUID histories and a bounded per-player audit trail, saved with the overworld. */
public final class AntiXrayData extends SavedData {
    public static final class Case {
        public String name="", fingerprint="", lastEvidence="";
        public final TrustHistory history=new TrustHistory();
        public final ArrayDeque<String> audit=new ArrayDeque<>();
        public int oreAlerts;
    }
    private final Map<UUID,Case> cases=new HashMap<>();
    private final Set<String> approvals=new HashSet<>();
    public static AntiXrayData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(AntiXrayData::new,AntiXrayData::load),"wayaround_antixray");
    }
    public Case get(UUID id) { return cases.computeIfAbsent(id,k->new Case()); }
    public Case find(UUID id) { return cases.get(id); }
    public boolean approved(String hash) { return approvals.contains(hash); }
    public void approve(String hash) { approvals.add(hash); setDirty(); }
    public void revoke(String hash) { approvals.remove(hash); setDirty(); }
    public void audit(UUID id,String message) {
        Case c=get(id);
        while (c.audit.size()>=64) c.audit.removeFirst();
        c.audit.addLast(java.time.Instant.now()+" "+message); setDirty();
    }
    public static AntiXrayData load(CompoundTag tag,HolderLookup.Provider registries) {
        AntiXrayData data=new AntiXrayData();
        CompoundTag all=tag.getCompound("cases");
        for (String key:all.getAllKeys()) {
            UUID id;
            try { id=UUID.fromString(key); } catch (IllegalArgumentException exception) { continue; }
            CompoundTag t=all.getCompound(key); Case c=data.get(id); TrustHistory h=c.history;
            c.name=t.getString("name"); c.fingerprint=t.getString("fingerprint"); c.lastEvidence=t.getString("evidence");
            h.evidenceSeconds=finite(t.getDouble("seconds"),86400); h.roundSeconds=finite(t.getDouble("round"),86400);
            h.lifetimeSeconds=finite(t.getDouble("lifetime"),31536000); h.updatedAt=t.getLong("updated");
            h.privateRound=finite(t.getDouble("privateRound"),86400);h.publicRound=finite(t.getDouble("publicRound"),86400);
            h.kicks=Math.max(0,Math.min(3,t.getInt("kicks"))); h.stage=Math.max(0,Math.min(2,t.getInt("stage"))); h.banned=t.getBoolean("banned");
            h.oreBreaks=Math.max(0,t.getInt("ores")); h.enclosedOreBreaks=Math.max(0,Math.min(h.oreBreaks,t.getInt("enclosed"))); c.oreAlerts=t.getInt("oreAlerts");
            ListTag audit=t.getList("audit",Tag.TAG_STRING);
            for (int i=Math.max(0,audit.size()-64);i<audit.size();i++) c.audit.add(audit.getString(i));
        }
        for (Tag value:tag.getList("approvals",Tag.TAG_STRING)) {
            String hash=value.getAsString(); if(hash.matches("[0-9a-f]{64}"))data.approvals.add(hash);
        }
        return data;
    }
    private static double finite(double value,double max) { return Double.isFinite(value)?Math.max(0,Math.min(max,value)):0; }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        CompoundTag all=new CompoundTag();
        cases.forEach((id,c)-> {
            TrustHistory h=c.history; CompoundTag t=new CompoundTag();
            t.putString("name",c.name);t.putString("fingerprint",c.fingerprint);t.putString("evidence",c.lastEvidence);
            t.putDouble("seconds",h.evidenceSeconds);t.putDouble("round",h.roundSeconds);t.putDouble("lifetime",h.lifetimeSeconds);t.putLong("updated",h.updatedAt);
            t.putDouble("privateRound",h.privateRound);t.putDouble("publicRound",h.publicRound);
            t.putInt("kicks",h.kicks);t.putInt("stage",h.stage);t.putBoolean("banned",h.banned);t.putInt("ores",h.oreBreaks);t.putInt("enclosed",h.enclosedOreBreaks);t.putInt("oreAlerts",c.oreAlerts);
            ListTag audit=new ListTag(); c.audit.forEach(line->audit.add(StringTag.valueOf(line)));t.put("audit",audit);all.put(id.toString(),t);
        });
        tag.put("cases",all); ListTag approved=new ListTag(); approvals.stream().sorted().forEach(hash->approved.add(StringTag.valueOf(hash)));tag.put("approvals",approved);return tag;
    }
}
