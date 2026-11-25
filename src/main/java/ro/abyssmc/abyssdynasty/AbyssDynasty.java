package ro.abyssmc.abyssdynasty;

import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import static net.minecraft.commands.Commands.*;

public final class AbyssDynasty implements ModInitializer {
 static final Path DIR=Path.of("config","abyssdynasty"), DATA=DIR.resolve("data.json");
 static final Gson G=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
 static final String PREFIX="&#00D6FF&lᴀ&#00CFFF&lʙ&#00C8FF&lʏ&#00C0FF&lꜱ&#00B8FF&lꜱ&#00B0FF&lᴅ&#00A8FF&lʏ&#009FFF&lɴ&#0096FF&lᴀ&#008DFF&lꜱ&#0083FF&lᴛ&#0079FF&lʏ";
 static final Map<String,String> DM=new LinkedHashMap<>(), DC=new LinkedHashMap<>(), DQ=new LinkedHashMap<>();
 static Map<String,String> messages,config,missionFile;
 static final Map<String,List<Mission>> missions=new LinkedHashMap<>();
 static State state=new State(); static MinecraftServer server; static boolean dirty; static int ticks;
 static final Map<UUID,Invite> invites=new HashMap<>();
 static final Map<UUID,Integer> fishBaseline=new HashMap<>();
 static final Map<UUID,String> lastPosition=new HashMap<>();
 static final Map<UUID,Integer> movingSeconds=new HashMap<>();
 // Immutable snapshot: TAB can request placeholders from a different thread.
 static volatile Map<UUID,String> placeholderNames=Map.of();
 static volatile String placeholderEmpty="Niciuna";
 static void refreshPlaceholderNames(){
     Map<UUID,String> next=new HashMap<>();
     for(Dynasty g:state.groups.values())for(UUID u:g.members.keySet())next.put(u,g.name);
     placeholderEmpty=t("placeholder-no-dynasty");
     placeholderNames=Map.copyOf(next);
 }
 static final class PlaceholderIntegration {
     static void register(){
         eu.pb4.placeholders.api.Placeholders.registerServer(
             net.minecraft.resources.Identifier.fromNamespaceAndPath("abyssdynasty","name"),
             (ctx,arg)->eu.pb4.placeholders.api.PlaceholderResult.value(
                 ctx.hasPlayer()?placeholderNames.getOrDefault(ctx.player().getUUID(),placeholderEmpty):placeholderEmpty));
     }
 }

 static class State {Map<String,Dynasty> groups=new LinkedHashMap<>(); Map<UUID,String> names=new HashMap<>(); Set<String> placed=new HashSet<>(); boolean scoreboard; Hologram hologram; Set<String> holoDimensions=new HashSet<>();}
 static class Hologram {String dim; double x,y,z;}
 static class Dynasty {String id,name; UUID owner; long xp; Map<UUID,String> members=new LinkedHashMap<>(); Map<String,Map<UUID,Long>> progress=new HashMap<>(); Map<String,Completion> completed=new HashMap<>();}
 static class Completion {long time,xp; Map<UUID,Long> contributors=new HashMap<>();}
 record Invite(String group,long until){}
 record Mission(String category,long goal,long xp){String id(){return category+"."+goal;}}
 static {String[][] pairs={
 {"prefix",PREFIX},{"no-group","&cNu faci parte dintr-o dinastie."},{"already-group","&cFaci deja parte dintr-o dinastie."},{"player-only","&cComanda este disponibila doar in joc."},{"not-leader","&cAi nevoie de permisiunea liderului."},{"name-invalid","&cNumele trebuie sa aiba 3-20 caractere (litere, cifre, _ sau -)."},{"name-taken","&cNumele este deja folosit."},{"created","&aAi creat dinastia &b{name}&a!"},{"offline","&cJucatorul nu este online."},{"full","&cDinastia a atins limita de membri."},{"invited","&aInvitatie trimisa lui {player}."},{"invite-received","&b{player} &fte-a invitat in &b{name}&f. Foloseste &a/dynasty accept&f."},{"invite-invalid","&cNu ai o invitatie valida."},{"joined","&a{player} a intrat in dinastie."},{"left","&e{player} a parasit dinastia."},{"leader-leave","&cTransfera conducerea sau foloseste /dynasty disband confirm."},{"not-member","&cJucatorul nu este membru valid."},{"removed","&e{player} a fost eliminat."},{"role-set","&aRolul lui {player} a fost schimbat."},{"transferred","&aNoul lider este {player}."},{"disbanded","&cDinastia a fost desfiintata."},{"reloaded","&aConfiguratia a fost reincarcata."},{"reload-failed","&cConfiguratie invalida. Verifica consola."},{"completed","&aChallenge finalizat: &b{category} {goal} &8| &6+{xp} Dynasty XP"},{"saved","&aChallenge salvat."},{"invalid-category","&cCategorie inexistenta."},{"invalid-mission","&cChallenge invalid sau deja existent."},{"credited","&aProgres inregistrat: {amount}."},{"score-on","&aScoreboard global activat."},{"score-off","&aScoreboard global dezactivat."},{"no-permission","&cNu ai permisiune."},
 {"gui-main","Meniu principal"},{"gui-missions","Misiuni"},{"gui-top","Clasament"},{"gui-members","Membri"},{"gui-details","Contributori"},{"gui-editor","Editor"},{"gui-history","Istoric"},{"gui-back","&cInapoi"},{"gui-prev","&eAnterior"},{"gui-next","&eUrmator"},{"gui-rank","&6Pozitie: #{rank}"},{"gui-xp","&bDynasty XP: {xp}"},{"gui-level","&aNivel: {level}"},{"gui-count","&7Membri: {count}/{max}"},{"gui-progress","&7Progres: &b{current} / {goal}"},{"gui-reward","&6Recompensa: {xp} XP"},{"gui-finished","&aFinalizat"},{"gui-active","&eIn progres"},{"gui-future","&7Viitor"},{"gui-challenges","&7Challenge-uri: &a{done}/{total}"},{"gui-contribution","&7Contributie: &b{amount}"},{"gui-mission-name","&b{category} - {goal}"},{"gui-create-help","&e/dynasty create <nume>"},{"gui-accept-help","&a/dynasty accept"},{"gui-player-role","&7Rol: {role}"},{"role-owner","Lider"},{"role-officer","Co-lider"},{"role-member","Membru"},{"gui-editor-add","&aAdauga urmatorul challenge"},{"gui-editor-hint","&7Click stanga +100 XP, dreapta -100 XP"},{"gui-editor-new","&7Adauga si prin /dynasty editor add"},{"gui-owner","&7Lider: {player}"},{"gui-empty","&7Nu exista date"},{"gui-view","&7Click pentru detalii"},{"category-build","Constructii"},{"category-mine","Minerit"},{"category-hunt","Vanatoare"},{"category-farm","Agricultura"},{"category-fish","Pescuit"},{"category-vote","Voturi"},{"category-crate","Crate-uri"},{"category-activity","Activitate"},{"category-explore","Explorare"},{"score-title","&b&lAbyssDynasty TOP"},{"holo-title","&b&lTOP DINASTII"},{"holo-line","&6#{rank} &f{name} &8- &b{xp} XP"},{"holo-empty","&7Nu exista dinastii"},{"holo-set","&aHolograma a fost setata."},{"holo-remove","&aHolograma a fost eliminata."}}; for(String[] a:pairs)DM.put(a[0],a[1]); DM.put("placeholder-no-dynasty","Niciuna");
 DC.put("max-members","10");DC.put("xp-per-level","10000");DC.put("invite-seconds","120");DC.put("track-placed-positions","true");DC.put("save-ticks","600");
 long[] goals={10000,20000,30000,50000,75000,100000,150000,200000,300000,400000,500000,600000,750000,900000,1000000};long[] mine={1000,2000,3000,5000,7500,10000,14000,18000,25000,32000,40000,47000,55000,63000,70000};long[] hunt={100,250,500,750,1000,1500,2000,3000,4000,5000,6000,7000,8000,9000,10000};long[] hx={250,500,1000,1500,2000,3000,4000,6000,8000,10000,12500,15000,20000,25000,30000};
 for(int i=0;i<goals.length;i++){DQ.put("build."+goals[i],""+(goals[i]/20));DQ.put("mine."+goals[i],""+mine[i]);DQ.put("hunt."+hunt[i],""+hx[i]);}
 for(String c:List.of("farm","fish","vote","crate","activity","explore"))for(int i=1;i<=5;i++){long goal=(c.equals("farm")?1000:c.equals("vote")?10:100)*i;DQ.put(c+"."+goal,""+(500*i));}
 messages=new LinkedHashMap<>(DM);config=new LinkedHashMap<>(DC);missionFile=new LinkedHashMap<>(DQ);
 }
 static void writeFlat(Path path,Map<String,String> map)throws Exception{StringBuilder b=new StringBuilder("# YAML plat: valori intre ghilimele duble.\n");map.forEach((k,v)->b.append(k).append(": ").append(G.toJson(v)).append('\n'));atomic(path,b.toString());}
 static void atomic(Path path,String text)throws Exception{Files.createDirectories(path.getParent());Path tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,text);try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}}
 static Map<String,String> readFlat(String name,Map<String,String> defaults,boolean merge)throws Exception{Path path=DIR.resolve(name);if(!Files.exists(path))writeFlat(path,defaults);Map<String,String> out=new LinkedHashMap<>();if(merge)out.putAll(defaults);for(String raw:Files.readAllLines(path)){String line=raw.trim();if(line.isEmpty()||line.startsWith("#"))continue;int n=line.indexOf(':');if(n<1)throw new IllegalArgumentException(name+": "+line);String k=line.substring(0,n).trim(),v=line.substring(n+1).trim();if(!v.startsWith("\"")||!v.endsWith("\""))throw new IllegalArgumentException(name+": "+k);out.put(k,G.fromJson(v,String.class));}return out;}
 static Map<String,List<Mission>> parseMissions(Map<String,String> raw){Map<String,List<Mission>> out=new LinkedHashMap<>();for(var e:raw.entrySet()){String[] bits=e.getKey().split("\\.");if(bits.length!=2||!bits[0].matches("[a-z][a-z0-9_-]{0,23}"))throw new IllegalArgumentException(e.getKey());long goal=Long.parseLong(bits[1]),xp=Long.parseLong(e.getValue());if(!bits[1].equals(Long.toString(goal))||goal<1||goal>1000000000L||xp<1||xp>1000000000L)throw new IllegalArgumentException(e.getKey());out.computeIfAbsent(bits[0],k->new ArrayList<>()).add(new Mission(bits[0],goal,xp));}for(var l:out.values())l.sort(Comparator.comparingLong(Mission::goal));return out;}
 static boolean loadConfig(){try{var m=readFlat("message.yml",DM,true);var c=readFlat("config.yml",DC,true);var q=readFlat("missions.yml",DQ,false);var parsed=parseMissions(q);for(String k:List.of("max-members","xp-per-level","invite-seconds","save-ticks"))if(Long.parseLong(c.get(k))<1||Long.parseLong(c.get(k))>Integer.MAX_VALUE)throw new IllegalArgumentException(k);messages=m;config=c;missionFile=q;missions.clear();missions.putAll(parsed);return true;}catch(Exception e){System.err.println("[AbyssDynasty] Config: "+e);return false;}}
 static void save(){if(!dirty)return;try{atomic(DATA,G.toJson(state));dirty=false;}catch(Exception e){System.err.println("[AbyssDynasty] Save: "+e);}}
 static void load(){try{if(Files.exists(DATA)){State x=G.fromJson(Files.readString(DATA),State.class);if(x==null)throw new IllegalStateException("Empty data");state=x;}}catch(Exception e){throw new IllegalStateException("AbyssDynasty data.json",e);}}
 static String t(String k,String... kv){String s=messages.getOrDefault(k,k);for(int i=0;i+1<kv.length;i+=2)s=s.replace("{"+kv[i]+"}",kv[i+1]);return s;}
 static String num(long n){return String.format(Locale.GERMANY,"%,d",n);}
 static String cat(String c){return messages.getOrDefault("category-"+c,c);}
 static int cfg(String k){return Integer.parseInt(config.get(k));}
 static Component text(String s){return HexText.parse(s);}
 static Component chat(String key,String... kv){return text(t("prefix")+" &8» &r"+t(key,kv));}
 static int reply(CommandSourceStack s,String k,String... kv){s.sendSuccess(()->chat(k,kv),false);return 1;}
 static int fail(CommandSourceStack s,String k){s.sendFailure(chat(k));return 0;}
 static void tell(ServerPlayer p,String k,String... kv){p.sendSystemMessage(chat(k,kv));}
 static ServerPlayer player(CommandSourceStack s){if(s.getEntity() instanceof ServerPlayer p)return p;fail(s,"player-only");return null;}
 static String name(ServerPlayer p){String n=p.getGameProfile().name();if(!n.equals(state.names.put(p.getUUID(),n)))dirty=true;return n;}
 static String name(UUID u){return state.names.getOrDefault(u,u.toString());}
 static Dynasty group(UUID u){for(Dynasty g:state.groups.values())if(g.members.containsKey(u))return g;return null;}
 static boolean manager(Dynasty g,UUID u){return g!=null&&(g.owner.equals(u)||"officer".equals(g.members.get(u)));}
 static int max(){return cfg("max-members");}
 static boolean admin(CommandSourceStack s){if(s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN))return true;if(!(s.getEntity() instanceof ServerPlayer p))return false;try{Object api=Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);Object m=Class.forName("net.luckperms.api.LuckPerms").getMethod("getUserManager").invoke(api);Object u=Class.forName("net.luckperms.api.model.user.UserManager").getMethod("getUser",UUID.class).invoke(m,p.getUUID());Object c=Class.forName("net.luckperms.api.model.PermissionHolder").getMethod("getCachedData").invoke(u);Object d=Class.forName("net.luckperms.api.cacheddata.CachedDataManager").getMethod("getPermissionData").invoke(c);return "TRUE".equals(String.valueOf(Class.forName("net.luckperms.api.cacheddata.CachedPermissionData").getMethod("checkPermission",String.class).invoke(d,"abyssdynasty.admin")));}catch(Exception e){return false;}}
 static ServerPlayer online(String n){return server==null?null:server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().name().equalsIgnoreCase(n)).findFirst().orElse(null);}
 static UUID member(Dynasty g,String n){return g.members.keySet().stream().filter(u->name(u).equalsIgnoreCase(n)).findFirst().orElse(null);}
 static void broadcast(Dynasty g,String k,String... kv){if(server!=null)for(ServerPlayer p:server.getPlayerList().getPlayers())if(g.members.containsKey(p.getUUID()))tell(p,k,kv);}
 static List<Dynasty> ranked(){return state.groups.values().stream().sorted(Comparator.comparingLong((Dynasty g)->g.xp).reversed().thenComparing(g->g.id)).toList();}
 static int rank(Dynasty g){return ranked().indexOf(g)+1;}
 static long total(Dynasty g,String c){return g.progress.getOrDefault(c,Map.of()).values().stream().mapToLong(Long::longValue).sum();}
 static boolean eligible(ServerPlayer p){return !p.isCreative()&&!p.isSpectator();}
 static String posKey(ServerPlayer p,BlockPos pos){return p.level().dimension().identifier()+":"+pos.asLong();}
 public static void placed(ServerPlayer p,BlockPos pos){if(server==null)return;boolean track=Boolean.parseBoolean(config.get("track-placed-positions"));boolean fresh=!track||state.placed.add(posKey(p,pos));if(track&&fresh)dirty=true;if(fresh&&eligible(p))credit(p,"build",1);}
 static void credit(ServerPlayer p,String category,long amount){Dynasty g=group(p.getUUID());if(g==null||amount<=0||!missions.containsKey(category))return;name(p);Map<UUID,Long> progress=g.progress.computeIfAbsent(category,k->new HashMap<>());long previous=total(g,category);long n=Math.min(amount,Math.max(0,1000000000000L-previous));if(n==0)return;progress.merge(p.getUUID(),n,Long::sum);dirty=true;for(Mission m:missions.get(category)){if(previous+n<m.goal||g.completed.containsKey(m.id()))continue;Completion c=new Completion();c.time=System.currentTimeMillis();c.xp=m.xp;c.contributors.putAll(progress);long excess=Math.min(n,Math.max(0,previous+n-m.goal));if(excess>0)c.contributors.computeIfPresent(p.getUUID(),(k,v)->v-excess);g.completed.put(m.id(),c);g.xp+=m.xp;broadcast(g,"completed","category",cat(category),"goal",num(m.goal),"xp",num(m.xp));} }
 @Override public void onInitialize(){if(!loadConfig())throw new IllegalStateException("AbyssDynasty invalid config");
 if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("placeholder-api")){
     try{PlaceholderIntegration.register();System.out.println("[AbyssDynasty] Registered %abyssdynasty:name%");}
     catch(LinkageError | RuntimeException e){System.err.println("[AbyssDynasty] Placeholder API integration failed: "+e);}
 }

 ServerLifecycleEvents.SERVER_STARTED.register(s->{server=s;state=new State();load();dirty=false;invites.clear();fishBaseline.clear();lastPosition.clear();movingSeconds.clear();ticks=0;refreshPlaceholderNames();});
 ServerLifecycleEvents.SERVER_STOPPING.register(s->{save();server=null;placeholderNames=Map.of();});
 ServerTickEvents.END_SERVER_TICK.register(s->{if(++ticks%cfg("save-ticks")==0)save();if(ticks%20==0){samplePlayers();refreshPlaceholderNames();}if(ticks%200==0&&state.scoreboard)refreshScore();if(ticks%600==0)refreshHolo();});
 PlayerBlockBreakEvents.AFTER.register((w,who,pos,block,be)->{if(who instanceof ServerPlayer p&&eligible(p)){if(!Boolean.parseBoolean(config.get("track-placed-positions"))||!state.placed.contains(posKey(p,pos)))credit(p,"mine",1);if(block.getBlock() instanceof CropBlock crop&&crop.isMaxAge(block))credit(p,"farm",1);}});
 ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(entity instanceof Monster&&source.getEntity() instanceof ServerPlayer p&&eligible(p))credit(p,"hunt",1);});
 CommandRegistrationCallback.EVENT.register((d,a,e)->{var root=commands();var node=d.register(root);d.register(literal("dy").redirect(node).executes(c->open(c.getSource(),"main")));});
 }
 static String str(CommandContext<CommandSourceStack> c,String n){return StringArgumentType.getString(c,n);}
 static LiteralArgumentBuilder<CommandSourceStack> commands(){var r=literal("dynasty").executes(c->open(c.getSource(),"main"));for(String v:List.of("top","missions","members","history"))r.then(literal(v).executes(c->open(c.getSource(),v)));
 r.then(literal("create").then(argument("name",StringArgumentType.word()).executes(c->create(c.getSource(),str(c,"name")))));
 for(String v:List.of("invite","kick","promote","demote","transfer"))r.then(literal(v).then(argument("player",StringArgumentType.word()).executes(c->memberAction(c.getSource(),v,str(c,"player")))));
 r.then(literal("accept").executes(c->accept(c.getSource())));r.then(literal("leave").executes(c->leave(c.getSource())));r.then(literal("disband").then(literal("confirm").executes(c->disband(c.getSource()))));
 r.then(literal("reload").requires(AbyssDynasty::admin).executes(c->loadConfig()?reply(c.getSource(),"reloaded"):fail(c.getSource(),"reload-failed")));
 r.then(literal("credit").requires(AbyssDynasty::admin).then(argument("player",StringArgumentType.word()).then(argument("category",StringArgumentType.word()).then(argument("amount",LongArgumentType.longArg(1,1000000000)).executes(c->{ServerPlayer p=online(str(c,"player"));String k=str(c,"category");if(p==null)return fail(c.getSource(),"offline");if(group(p.getUUID())==null)return fail(c.getSource(),"no-group");if(!missions.containsKey(k))return fail(c.getSource(),"invalid-category");long n=LongArgumentType.getLong(c,"amount");credit(p,k,n);save();return reply(c.getSource(),"credited","amount",num(n));})))));
 var ed=literal("editor").requires(AbyssDynasty::admin).executes(c->open(c.getSource(),"editor"));ed.then(literal("add").then(argument("category",StringArgumentType.word()).then(argument("goal",LongArgumentType.longArg(1,1000000000)).then(argument("xp",LongArgumentType.longArg(1,1000000000)).executes(c->{String k=str(c,"category");long g=LongArgumentType.getLong(c,"goal"),x=LongArgumentType.getLong(c,"xp");if(missionFile.containsKey(k+"."+g)||!edit(k,g,x))return fail(c.getSource(),"invalid-mission");return reply(c.getSource(),"saved");})))));r.then(ed);
 r.then(literal("scoreboard").requires(AbyssDynasty::admin).then(literal("on").executes(c->{state.scoreboard=true;dirty=true;save();run("scoreboard objectives add abyssdynasty dummy "+G.toJson(t("score-title").replaceAll("&[0-9a-fklmnor]","")));refreshScore();run("scoreboard objectives setdisplay sidebar abyssdynasty");return reply(c.getSource(),"score-on");})).then(literal("off").executes(c->{state.scoreboard=false;dirty=true;save();run("scoreboard objectives remove abyssdynasty");return reply(c.getSource(),"score-off");})));
 r.then(literal("hologram").requires(AbyssDynasty::admin).then(literal("set").executes(c->{ServerPlayer p=player(c.getSource());if(p==null)return 0;Hologram h=new Hologram();h.dim=p.level().dimension().identifier().toString();h.x=p.position().x;h.y=p.position().y+2.2;h.z=p.position().z;state.holoDimensions.add(h.dim);state.hologram=h;dirty=true;save();refreshHolo();return reply(c.getSource(),"holo-set");})).then(literal("remove").executes(c->{state.hologram=null;dirty=true;save();refreshHolo();return reply(c.getSource(),"holo-remove");})));
 return r;}
 static int create(CommandSourceStack s,String n){ServerPlayer p=player(s);if(p==null)return 0;if(group(p.getUUID())!=null)return fail(s,"already-group");if(!n.matches("[A-Za-z0-9_-]{3,20}"))return fail(s,"name-invalid");if(state.groups.values().stream().anyMatch(g->g.name.equalsIgnoreCase(n)))return fail(s,"name-taken");name(p);Dynasty g=new Dynasty();g.id=UUID.randomUUID().toString();g.name=n;g.owner=p.getUUID();g.members.put(g.owner,"owner");fishBaseline.remove(p.getUUID());movingSeconds.remove(p.getUUID());state.groups.put(g.id,g);dirty=true;save();reply(s,"created","name",n);main(p);return 1;}
 static int memberAction(CommandSourceStack s,String action,String n){ServerPlayer p=player(s);if(p==null)return 0;Dynasty g=group(p.getUUID());if(g==null)return fail(s,"no-group");boolean owner=g.owner.equals(p.getUUID());if(!manager(g,p.getUUID())||(!owner&&!action.equals("invite")&&!action.equals("kick")))return fail(s,"not-leader");if(action.equals("invite")){ServerPlayer to=online(n);if(to==null)return fail(s,"offline");if(group(to.getUUID())!=null)return fail(s,"already-group");if(g.members.size()>=max())return fail(s,"full");name(to);invites.put(to.getUUID(),new Invite(g.id,System.currentTimeMillis()+cfg("invite-seconds")*1000L));tell(to,"invite-received","player",name(p),"name",g.name);return reply(s,"invited","player",name(to));}UUID u=member(g,n);if(u==null||u.equals(g.owner)||u.equals(p.getUUID())||(!owner&&"officer".equals(g.members.get(u))))return fail(s,"not-member");switch(action){case "kick"->{g.members.remove(u);ServerPlayer to=online(n);if(to!=null)tell(to,"removed","player",name(u));broadcast(g,"removed","player",name(u));}case "promote"->g.members.put(u,"officer");case "demote"->g.members.put(u,"member");case "transfer"->{g.members.put(g.owner,"officer");g.owner=u;g.members.put(u,"owner");broadcast(g,"transferred","player",name(u));}}dirty=true;save();return reply(s,"role-set","player",name(u));}
 static int accept(CommandSourceStack s){ServerPlayer p=player(s);if(p==null)return 0;if(group(p.getUUID())!=null)return fail(s,"already-group");Invite i=invites.remove(p.getUUID());Dynasty g=i==null?null:state.groups.get(i.group);if(g==null||i.until<System.currentTimeMillis())return fail(s,"invite-invalid");if(g.members.size()>=max())return fail(s,"full");name(p);g.members.put(p.getUUID(),"member");fishBaseline.remove(p.getUUID());movingSeconds.remove(p.getUUID());dirty=true;save();broadcast(g,"joined","player",name(p));return 1;}
 static int leave(CommandSourceStack s){ServerPlayer p=player(s);if(p==null)return 0;Dynasty g=group(p.getUUID());if(g==null)return fail(s,"no-group");if(g.owner.equals(p.getUUID()))return fail(s,"leader-leave");g.members.remove(p.getUUID());dirty=true;save();broadcast(g,"left","player",name(p));return reply(s,"left","player",name(p));}
 static int disband(CommandSourceStack s){ServerPlayer p=player(s);if(p==null)return 0;Dynasty g=group(p.getUUID());if(g==null)return fail(s,"no-group");if(!g.owner.equals(p.getUUID()))return fail(s,"not-leader");broadcast(g,"disbanded");state.groups.remove(g.id);dirty=true;save();return 1;}
 static boolean edit(String category,long goal,long xp){try{Map<String,String> copy=new LinkedHashMap<>(missionFile);copy.put(category+"."+goal,""+xp);var next=parseMissions(copy);writeFlat(DIR.resolve("missions.yml"),copy);missionFile=copy;missions.clear();missions.putAll(next);return true;}catch(Exception e){System.err.println(e);return false;}}
 static void run(String cmd){if(server!=null)try{server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),cmd);}catch(Exception e){System.err.println("[AbyssDynasty] command: "+e);}}
 static void samplePlayers(){if(server==null)return;Set<UUID> online=new HashSet<>();for(ServerPlayer p:server.getPlayerList().getPlayers()){UUID u=p.getUUID();online.add(u);name(p);int fish=p.getStats().getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT));Integer before=fishBaseline.put(u,fish);String pos=posKey(p,p.blockPosition());String old=lastPosition.put(u,pos);if(!eligible(p)||group(u)==null){movingSeconds.remove(u);continue;}if(before!=null&&fish>before)credit(p,"fish",fish-before);if(old!=null&&!old.equals(pos)){int seconds=movingSeconds.merge(u,1,Integer::sum);if(seconds>=60){movingSeconds.remove(u);credit(p,"activity",1);}}}fishBaseline.keySet().retainAll(online);lastPosition.keySet().retainAll(online);movingSeconds.keySet().retainAll(online);}
 static void refreshHolo(){for(String dim:state.holoDimensions)run("execute in "+dim+" run kill @e[type=minecraft:text_display,tag=abyssdynasty_holo]");Hologram h=state.hologram;if(h==null)return;List<String> lines=new ArrayList<>();lines.add(t("holo-title"));List<Dynasty> list=ranked();for(int i=0;i<Math.min(10,list.size());i++){Dynasty g=list.get(i);lines.add(t("holo-line","rank",""+(i+1),"name",g.name,"xp",num(g.xp)));}if(list.isEmpty())lines.add(t("holo-empty"));for(int i=0;i<lines.size();i++){String nbt="{Tags:[\"abyssdynasty_holo\"],billboard:\"center\",shadow:1b,text:"+hologramComponent(lines.get(i),"#FFFFFF")+"}";run("execute in "+h.dim+" positioned "+h.x+" "+h.y+" "+h.z+" if entity @a[distance=..64] run summon minecraft:text_display "+h.x+" "+(h.y-i*0.27)+" "+h.z+" "+nbt);}}

    static String snbtQuote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static String hologramComponent(String text, String fallbackColor) {
        // 3R.5.1: Support legacy & colors, HEX, formatting and Birdflop gradients in Text Displays.
        // Build a structured SNBT component rather than displaying formatting tokens literally.
        Matcher gradient = Pattern.compile("(?i)<gradient:#([0-9a-f]{6}):#([0-9a-f]{6})>(.*?)</gradient>", Pattern.DOTALL).matcher(text);
        StringBuffer expanded = new StringBuffer();
        while (gradient.find()) {
            String body = gradient.group(3);
            int from = Integer.parseInt(gradient.group(1), 16);
            int to = Integer.parseInt(gradient.group(2), 16);
            int count = body.codePointCount(0, body.length());
            StringBuilder coloredBody = new StringBuilder();
            int i = 0;
            for (int off = 0; off < body.length();) {
                int cp = body.codePointAt(off);
                double t = count <= 1 ? 0 : (double)i / (count - 1);
                int r = (int)Math.round(((from >> 16) & 255) * (1-t) + ((to >> 16) & 255) * t);
                int g = (int)Math.round(((from >> 8) & 255) * (1-t) + ((to >> 8) & 255) * t);
                int bl = (int)Math.round((from & 255) * (1-t) + (to & 255) * t);
                coloredBody.append(String.format(Locale.ROOT, "&#%02X%02X%02X", r, g, bl));
                coloredBody.appendCodePoint(cp);
                off += Character.charCount(cp);
                i++;
            }
            gradient.appendReplacement(expanded, Matcher.quoteReplacement(coloredBody.toString()));
        }
        gradient.appendTail(expanded);
        text = expanded.toString();

        List<String> segments = new ArrayList<>();
        Matcher token = Pattern.compile("(?i)&#([0-9a-f]{6})|&([0-9a-fk-or])").matcher(text);
        String color = fallbackColor;
        boolean bold = false, italic = false, underlined = false, strikethrough = false, obfuscated = false;
        int start = 0;
        while (token.find()) {
            if (token.start() > start) {
                segments.add(hologramSegment(text.substring(start, token.start()), color, bold, italic, underlined, strikethrough, obfuscated));
            }
            if (token.group(1) != null) {
                color = "#" + token.group(1).toUpperCase(Locale.ROOT);
                bold = italic = underlined = strikethrough = obfuscated = false;
            } else {
                char code = Character.toLowerCase(token.group(2).charAt(0));
                switch (code) {
                    case 'l' -> bold = true;
                    case 'o' -> italic = true;
                    case 'n' -> underlined = true;
                    case 'm' -> strikethrough = true;
                    case 'k' -> obfuscated = true;
                    case 'r' -> { color = fallbackColor; bold = italic = underlined = strikethrough = obfuscated = false; }
                    default -> {
                        color = switch (code) {
                            case '0' -> "#000000"; case '1' -> "#0000AA"; case '2' -> "#00AA00";
                            case '3' -> "#00AAAA"; case '4' -> "#AA0000"; case '5' -> "#AA00AA";
                            case '6' -> "#FFAA00"; case '7' -> "#AAAAAA"; case '8' -> "#555555";
                            case '9' -> "#5555FF"; case 'a' -> "#55FF55"; case 'b' -> "#55FFFF";
                            case 'c' -> "#FF5555"; case 'd' -> "#FF55FF"; case 'e' -> "#FFFF55";
                            case 'f' -> "#FFFFFF"; default -> fallbackColor;
                        };
                        bold = italic = underlined = strikethrough = obfuscated = false;
                    }
                }
            }
            start = token.end();
        }
        if (start < text.length()) {
            segments.add(hologramSegment(text.substring(start), color, bold, italic, underlined, strikethrough, obfuscated));
        }
        if (segments.isEmpty()) return "{text:\"\"}";
        if (segments.size() == 1) return segments.get(0);
        return "{text:\"\",extra:[" + String.join(",", segments) + "]}";
    }

    static String hologramSegment(String value, String color, boolean bold, boolean italic,
                                          boolean underlined, boolean strikethrough, boolean obfuscated) {
        return "{text:" + snbtQuote(value) + ",color:" + snbtQuote(color)
                + ",bold:" + bold + ",italic:" + italic + ",underlined:" + underlined
                + ",strikethrough:" + strikethrough + ",obfuscated:" + obfuscated + "}";
    }

 static void refreshScore(){run("scoreboard players reset * abyssdynasty");List<Dynasty> list=ranked();for(int i=0;i<Math.min(10,list.size());i++){Dynasty g=list.get(i);run("scoreboard players set "+g.name+" abyssdynasty "+Math.min(Integer.MAX_VALUE,g.xp));}}
 static int open(CommandSourceStack s,String tab){ServerPlayer p=player(s);if(p==null)return 0;switch(tab){case "main"->main(p);case "top"->top(p,0);case "editor"->categories(p,true);default->{Dynasty g=group(p.getUUID());if(g==null)return fail(s,"no-group");switch(tab){case "missions"->categories(p,false);case "members"->members(p,g,0,()->main(p));case "history"->history(p,g,0);}}}return 1;}
 static ItemStack icon(Item item,String title,String... lore){ItemStack s=new ItemStack(item);s.set(DataComponents.CUSTOM_NAME,text(title));if(lore.length>0)s.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(AbyssDynasty::text).toList()));return s;}
 static class Gui {final SimpleContainer inv=new SimpleContainer(54);final Map<Integer,IntConsumer> actions=new HashMap<>();final ServerPlayer p;final BooleanSupplier valid;final String title;
 Gui(ServerPlayer p,String title,BooleanSupplier valid){this.p=p;this.title=title;this.valid=valid;}
 void item(int slot,Item item,String title,Runnable action,String... lore){inv.setItem(slot,icon(item,title,lore));if(action!=null)actions.put(slot,b->action.run());}
 void show(){p.openMenu(new SimpleMenuProvider((id,pi,who)->new ChestMenu(MenuType.GENERIC_9x6,id,pi,inv,6){@Override public void clicked(int slot,int button,ContainerInput type,Player who){if(!valid.getAsBoolean()||who!=p||slot<0||slot>=54||!"PICKUP".equals(type.name()))return;IntConsumer action=actions.get(slot);if(action!=null)action.accept(button);}@Override public ItemStack quickMoveStack(Player who,int slot){return ItemStack.EMPTY;}@Override public boolean stillValid(Player who){return who==p&&valid.getAsBoolean();}},text(t("prefix")+" &8| &b"+title)));}
 void nav(int page,int size,Runnable previous,Runnable next,Runnable back){if(page>0)item(45,Items.ARROW,t("gui-prev"),previous);if((page+1)*45<size)item(53,Items.ARROW,t("gui-next"),next);item(49,Items.BARRIER,t("gui-back"),back);}
 }
 static BooleanSupplier own(ServerPlayer p,Dynasty g){return ()->group(p.getUUID())==g&&state.groups.get(g.id)==g;}
 static String[] stats(Dynasty g){return new String[]{t("gui-rank","rank",""+rank(g)),t("gui-xp","xp",num(g.xp)),t("gui-level","level",num(1+g.xp/cfg("xp-per-level"))),t("gui-count","count",""+g.members.size(),"max",""+max()),t("gui-owner","player",name(g.owner))};}
 static void main(ServerPlayer p){Dynasty g=group(p.getUUID());Gui ui=new Gui(p,t("gui-main"),()->true);if(g==null){ui.item(13,Items.BOOK,t("gui-create-help"),null,t("gui-accept-help"));ui.item(22,Items.GOLD_BLOCK,t("gui-top"),()->top(p,0));}else{ui.item(13,Items.NETHER_STAR,"&6&l"+g.name,null,stats(g));ui.item(20,Items.BOOK,t("gui-missions"),()->categories(p,false));ui.item(22,Items.GOLD_BLOCK,t("gui-top"),()->top(p,0));ui.item(24,Items.PLAYER_HEAD,t("gui-members"),()->members(p,g,0,()->main(p)));ui.item(31,Items.WRITABLE_BOOK,t("gui-history"),()->history(p,g,0));}if(admin(p.createCommandSourceStack()))ui.item(40,Items.REDSTONE,t("gui-editor"),()->categories(p,true));ui.show();}
 static Item categoryItem(String c){return switch(c){case "build"->Items.BRICKS;case "mine"->Items.DIAMOND_PICKAXE;case "hunt"->Items.DIAMOND_SWORD;case "farm"->Items.WHEAT;case "fish"->Items.FISHING_ROD;case "vote"->Items.DIAMOND;case "crate"->Items.CHEST;case "activity"->Items.CLOCK;case "explore"->Items.COMPASS;default->Items.BOOK;};}
 static void categories(ServerPlayer p,boolean editor){categoryPage(p,editor,0);}
 static void categoryPage(ServerPlayer p,boolean editor,int page){Dynasty g=group(p.getUUID());if(!editor&&g==null){main(p);return;}if(editor&&!admin(p.createCommandSourceStack()))return;Gui ui=new Gui(p,t(editor?"gui-editor":"gui-missions"),editor?()->admin(p.createCommandSourceStack()):own(p,g));List<String> cats=new ArrayList<>(missions.keySet());for(int i=0;i<45&&page*45+i<cats.size();i++){String c=cats.get(page*45+i);List<Mission> list=missions.get(c);long done=g==null?0:list.stream().filter(m->g.completed.containsKey(m.id())).count();ui.item(i,categoryItem(c),"&b&l"+cat(c),()->missionPage(p,c,editor,0),t("gui-challenges","done",num(done),"total",""+list.size()));}ui.nav(page,cats.size(),()->categoryPage(p,editor,page-1),()->categoryPage(p,editor,page+1),()->main(p));if(editor)ui.item(50,Items.BOOK,t("gui-editor-new"),null);ui.show();}
 static void missionPage(ServerPlayer p,String c,boolean editor,int page){Dynasty g=group(p.getUUID());if(!editor&&g==null){main(p);return;}if(editor&&!admin(p.createCommandSourceStack()))return;Gui ui=new Gui(p,cat(c),editor?()->admin(p.createCommandSourceStack()):own(p,g));List<Mission> list=missions.getOrDefault(c,List.of());long current=g==null?0:total(g,c);boolean activeFound=false;for(int j=0;j<list.size();j++){Mission m=list.get(j);boolean done=g!=null&&g.completed.containsKey(m.id());boolean active=!done&&!activeFound;if(active)activeFound=true;int i=j-page*45;if(i<0||i>=45)continue;String label=t("gui-mission-name","category",cat(c),"goal",num(m.goal));String reward=t("gui-reward","xp",num(done?g.completed.get(m.id()).xp:m.xp));if(editor){ui.item(i,Items.BOOK,label,null,reward,t("gui-editor-hint"));ui.actions.put(i,b->{if(!admin(p.createCommandSourceStack()))return;Mission latest=missions.getOrDefault(c,List.of()).stream().filter(x->x.goal==m.goal).findFirst().orElse(null);if(latest!=null&&edit(c,m.goal,Math.max(1,Math.min(1000000000L,latest.xp+(b==1?-100:100)))))missionPage(p,c,true,page);});}else{ui.item(i,done?Items.EMERALD_BLOCK:active?Items.GOLD_BLOCK:Items.IRON_BLOCK,label,()->detail(p,g,m,0,()->missionPage(p,c,false,page)),t(done?"gui-finished":active?"gui-active":"gui-future"),t("gui-progress","current",num(Math.min(current,m.goal)),"goal",num(m.goal)),reward);if(active)ui.inv.getItem(i).set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);}}
 ui.nav(page,list.size(),()->missionPage(p,c,editor,page-1),()->missionPage(p,c,editor,page+1),()->categories(p,editor));if(editor)ui.item(50,Items.EMERALD,t("gui-editor-add"),()->{if(!admin(p.createCommandSourceStack()))return;List<Mission> latest=missions.getOrDefault(c,List.of());Mission last=latest.isEmpty()?new Mission(c,0,100):latest.get(latest.size()-1);long goal=last.goal+Math.max(1,last.goal/10);if(edit(c,goal,last.xp))missionPage(p,c,true,latest.size()/45);});ui.show();}
 static void detail(ServerPlayer p,Dynasty g,Mission m,int page,Runnable back){if(!own(p,g).getAsBoolean()){main(p);return;}Gui ui=new Gui(p,t("gui-details"),own(p,g));Completion completed=g.completed.get(m.id());Map<UUID,Long> map=completed==null?g.progress.getOrDefault(m.category,Map.of()):completed.contributors;List<Map.Entry<UUID,Long>> order=map.entrySet().stream().filter(e->e.getValue()>0).sorted(Map.Entry.<UUID,Long>comparingByValue().reversed().thenComparing(e->e.getKey().toString())).toList();long progress=completed==null?Math.min(total(g,m.category),m.goal):m.goal;for(int i=0;i<45&&page*45+i<order.size();i++){var e=order.get(page*45+i);ui.item(i,Items.PLAYER_HEAD,"&6#"+(page*45+i+1)+" &f"+name(e.getKey()),null,t("gui-contribution","amount",num(e.getValue())));}ui.nav(page,order.size(),()->detail(p,g,m,page-1,back),()->detail(p,g,m,page+1,back),back);ui.item(47,Items.BOOK,t("gui-mission-name","category",cat(m.category),"goal",num(m.goal)),null,t("gui-progress","current",num(progress),"goal",num(m.goal)),t("gui-reward","xp",num(completed==null?m.xp:completed.xp)),t(completed==null?"gui-active":"gui-finished"));ui.show();}
 static void top(ServerPlayer p,int page){Gui ui=new Gui(p,t("gui-top"),()->true);List<Dynasty> list=ranked();for(int i=0;i<45&&page*45+i<list.size();i++){Dynasty g=list.get(page*45+i);int index=page*45+i;ui.item(i,index==0?Items.GOLD_BLOCK:index==1?Items.IRON_BLOCK:index==2?Items.BRICK:Items.PLAYER_HEAD,"&6#"+(index+1)+" &b"+g.name,()->members(p,g,0,()->top(p,page)),stats(g));}ui.nav(page,list.size(),()->top(p,page-1),()->top(p,page+1),()->main(p));ui.show();}
 static void members(ServerPlayer p,Dynasty g,int page,Runnable back){if(!state.groups.containsKey(g.id)){main(p);return;}Gui ui=new Gui(p,t("gui-members")+" - "+g.name,()->state.groups.get(g.id)==g);List<UUID> list=new ArrayList<>(g.members.keySet());for(int i=0;i<45&&page*45+i<list.size();i++){UUID u=list.get(page*45+i);long sum=g.progress.values().stream().mapToLong(m->m.getOrDefault(u,0L)).sum();ui.item(i,Items.PLAYER_HEAD,"&b"+name(u),null,t("gui-player-role","role",t("role-"+g.members.get(u))),t("gui-contribution","amount",num(sum)));}ui.nav(page,list.size(),()->members(p,g,page-1,back),()->members(p,g,page+1,back),back);ui.show();}
 static void history(ServerPlayer p,Dynasty g,int page){if(!own(p,g).getAsBoolean()){main(p);return;}Gui ui=new Gui(p,t("gui-history"),own(p,g));List<String> ids=g.completed.keySet().stream().sorted(Comparator.<String>comparingLong(k->g.completed.get(k).time).reversed().thenComparing(k->k)).toList();for(int i=0;i<45&&page*45+i<ids.size();i++){String id=ids.get(page*45+i);Completion x=g.completed.get(id);String[] parts=id.split("\\.");Mission m=new Mission(parts[0],Long.parseLong(parts[1]),x.xp);ui.item(i,Items.EMERALD_BLOCK,t("gui-mission-name","category",cat(m.category),"goal",num(m.goal)),()->detail(p,g,m,0,()->history(p,g,page)),t("gui-reward","xp",num(x.xp)));}ui.nav(page,ids.size(),()->history(p,g,page-1),()->history(p,g,page+1),()->main(p));ui.show();}
}
