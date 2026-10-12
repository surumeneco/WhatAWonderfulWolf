package co.surumene.www.probe;

import co.surumene.www.breeding.*;
import co.surumene.www.config.*;
import co.surumene.www.domain.*;
import co.surumene.www.founder.*;
import co.surumene.www.genome.*;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;
import co.surumene.wgl.core.*;
import com.sun.management.ThreadMXBean;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

final class GenomePerformanceProbeTest {
    private final WwwConfig config=WwwConfigLoader.loadDefaults();
    private final WonderfulGenomeEngine engine=WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile=new WonderfulWolfGenomeProfile(config,engine.geneSequenceCodec());
    private final WonderfulWolfFounderSynthesizer founder=new WonderfulWolfFounderSynthesizer(engine,profile);
    private final WonderfulWolfBreedingService breeding=new WonderfulWolfBreedingService(engine,()->profile);
    private final ThreadMXBean mx=(ThreadMXBean)ManagementFactory.getThreadMXBean();

    private <T> T time(String stage,long seed,Supplier<T> task) {
        Thread owner=Thread.currentThread();long id=owner.threadId();
        AtomicBoolean active=new AtomicBoolean(true);
        Map<String,Integer> counts=new HashMap<>();
        Thread sample=new Thread(()->{while(active.get()){
            for(StackTraceElement f:owner.getStackTrace()) if(f.getClassName().startsWith("co.surumene.wgl.core.")){
                counts.merge(f.getClassName().substring("co.surumene.wgl.core.".length())+"."+f.getMethodName(),1,Integer::sum);
                break;
            }
            try {Thread.sleep(2);}catch(InterruptedException e){break;}
        }},"wgl-profile-sample");
        sample.setDaemon(true);
        long b0=mx.isThreadAllocatedMemorySupported()?mx.getThreadAllocatedBytes(id):-1;
        long c0=mx.isThreadCpuTimeSupported()?mx.getThreadCpuTime(id):-1;
        long n0=System.nanoTime();
        sample.start();T result;
        try {result=task.get();}
        finally {active.set(false);try{sample.join(500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        double ms=(System.nanoTime()-n0)/1e6;
        double cpu=c0<0?-1:(mx.getThreadCpuTime(id)-c0)/1e6;
        double mb=b0<0?-1:(mx.getThreadAllocatedBytes(id)-b0)/1048576.0;
        String hot=counts.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
            .limit(7).map(e->e.getKey()+"="+e.getValue()).reduce((a,b)->a+";"+b).orElse("-");
        System.out.printf(Locale.ROOT,"PERF species=WWW stage=%s seed=%d wall_ms=%.2f cpu_ms=%.2f alloc_MiB=%.2f samples=%d hotspots=%s%n",
            stage,seed,ms,cpu,mb,counts.values().stream().mapToInt(Integer::intValue).sum(),hot);
        return result;
    }

    private WonderfulWolfIndividual individual(FounderGenomeSynthesis run,FounderOrigin origin) {
        SynthesisResult.Success synthesized=assertInstanceOf(SynthesisResult.Success.class,run.result());
        DiploidGenome genome=synthesized.genome();
        DecodeResult<WonderfulWolfDecodedPhenotype> canonical=engine.decode(profile,genome);
        return new WonderfulWolfIndividual(
            genome,canonical.phenotype().toSnapshot(canonical.identity(),
                origin==FounderOrigin.NATURAL?PhenotypeOrigin.NATURAL_FOUNDER:PhenotypeOrigin.WOLF_TRAP_FOUNDER),
            Optional.empty(),0L,Mode.WANDER,Optional.empty(),ActionDistance.NORMAL,
            Optional.empty(),Map.of(),Optional.empty(),Map.of(),0,PedigreeSnapshot.founder());
    }

    private static int windowError(BitSequence child,ChromosomePair origin,int pos) {
        long bits=child.toLong(pos,48);
        return Math.min(Long.bitCount(bits^origin.haplotypeA().toLong(pos,48)),
             Long.bitCount(bits^origin.haplotypeB().toLong(pos,48)));
    }
    private static double[] similarity(DiploidGenome child,DiploidGenome a,DiploidGenome b,DiploidGenome unrelated) {
        double family=0,base=0,fExact=0,uExact=0;int count=0;
        for(int i=0;i<child.chromosomePairs().size();i++){
            ChromosomePair c=child.chromosomePairs().get(i),pA=a.chromosomePairs().get(i),pB=b.chromosomePairs().get(i),pU=unrelated.chromosomePairs().get(i);
            for(int side=0;side<2;side++){
                BitSequence h=side==0?c.haplotypeA():c.haplotypeB();
                ChromosomePair p=side==0?pA:pB;
                int max=Math.min(h.bitLength(),Math.min(p.haplotypeA().bitLength(),Math.min(p.haplotypeB().bitLength(),Math.min(pU.haplotypeA().bitLength(),pU.haplotypeB().bitLength()))));
                for(int pos=0;pos+48<=max;pos+=96){
                    int f=windowError(h,p,pos),u=windowError(h,pU,pos);
                    family+=f;base+=u;if(f==0)fExact++;if(u==0)uExact++;count++;
                }
            }
        }
        return new double[]{family/count,base/count,fExact/count,uExact/count,count};
    }
    private static double mae(Map<Ability,Double> child,Map<Ability,Double> a,Map<Ability,Double> b) {
        double error=0;for(Ability key:Ability.values())error+=Math.abs(child.get(key)-0.5*(a.get(key)+b.get(key)));
        return error/Ability.values().length;
    }
    @Test void executeProductionFounderAndBreedingPaths(){
        List<WonderfulWolfIndividual> parents=new ArrayList<>();
        for(int i=0;i<3;i++){
            long seed=20261012000L+i;
            FounderOrigin origin=i==2?FounderOrigin.WOLF_TRAP:FounderOrigin.NATURAL;
            FounderGenomeSynthesis run=time("founder_"+origin,seed,()->founder.synthesize(origin,seed));
            parents.add(individual(run,origin));
        }
        WonderfulWolfIndividual a=parents.get(0),b=parents.get(1),unrelated=parents.get(2);
        double d=0,u=0,e=0,ue=0,m=0,um=0;int n=0;
        for(int i=0;i<3;i++){
            long seed=20261012200L+i;
            WonderfulWolfBreedingOutcome outcome=time("breeding",seed,()->breeding.breed(
                new BreedingParent("A",a),new BreedingParent("B",b),Optional.empty(),seed));
            WonderfulWolfBreedingOutcome.Success success=assertInstanceOf(WonderfulWolfBreedingOutcome.Success.class,outcome);
            WonderfulWolfIndividual c=success.child();
            double[] g=similarity(c.genome(),a.genome(),b.genome(),unrelated.genome());
            double p=mae(c.phenotypeSnapshot().abilities(),a.phenotypeSnapshot().abilities(),b.phenotypeSnapshot().abilities());
            double q=mae(c.phenotypeSnapshot().abilities(),unrelated.phenotypeSnapshot().abilities(),unrelated.phenotypeSnapshot().abilities());
            System.out.printf(Locale.ROOT,"HEREDITY species=WWW child=%d family_bit_error_48=%.4f unrelated_bit_error_48=%.4f family_exact48=%.5f unrelated_exact48=%.5f windows=%d phenotype_midparent_MAE=%.5f phenotype_unrelated_MAE=%.5f%n",
                i,g[0],g[1],g[2],g[3],(int)g[4],p,q);
            d+=g[0];u+=g[1];e+=g[2];ue+=g[3];m+=p;um+=q;n++;
        }
        System.out.printf(Locale.ROOT,"HEREDITY_SUMMARY species=WWW children=%d mean_family_bit_error_48=%.4f mean_unrelated_bit_error_48=%.4f family_exact48=%.5f unrelated_exact48=%.5f mean_midparent_MAE=%.5f mean_unrelated_MAE=%.5f%n",
            n,d/n,u/n,e/n,ue/n,m/n,um/n);
    }
}
