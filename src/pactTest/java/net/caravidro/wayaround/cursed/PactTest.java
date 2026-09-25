package net.caravidro.wayaround.cursed;
import net.caravidro.wayaround.cursed.SpokenPactDraft;
public class PactTest {
 static void check(boolean b) {if(!b) throw new AssertionError();}
 public static void main(String[] args) {
  var d=new SpokenPactDraft();
  d.append("vamos fazer um trato"); check(!d.ready()); check(!d.forget());
  d.append("3 segundos"); check(d.ready() && d.durationSeconds()==3);
  d.append("na verdade três minutos"); check(d.durationSeconds()==180);
  d.append("não posso atacar ninguém"); check(d.pacifist());
  d.append("posso atacar"); check(!d.pacifist());
  d.append("você não vai lembrar"); check(d.forget());
  d.append("sem esquecimento"); check(!d.forget());
  d.append("você esquecerá esse trato"); check(d.forget());
  d.append("não esquecer"); check(!d.forget());
  d.append("um minuto e trinta segundos"); check(d.durationSeconds()==90);
  d.append("vinte e três segundos"); check(d.durationSeconds()==23);
  d.append("0 segundos"); check(!d.ready());
  d.append("999999999999999999 segundos"); check(!d.ready());
  d.append("3 segundos"); check(d.ready());
  d.append("fuga"); check(d.transcript().contains("fuga"));
  for(int i=0;i<9000;i++) d.append("a"); check(d.transcript().length()<=8192);
  System.out.println("Pact parser: 17 checks passed");
 }
}
