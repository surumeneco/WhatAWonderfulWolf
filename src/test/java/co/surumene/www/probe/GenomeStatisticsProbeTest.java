package co.surumene.www.probe;

import co.surumene.genomestats.GenomeStats;
import co.surumene.www.breeding.*;
import co.surumene.www.config.*;
import co.surumene.www.domain.*;
import co.surumene.www.founder.*;
import co.surumene.www.genome.*;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;
import co.surumene.wgl.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class GenomeStatisticsProbeTest {
    private final WwwConfig config=WwwConfigLoader.loadDefaults();
    private final WonderfulGenomeEngine engine=WonderfulGenomeEngine.create(EngineConfig.defaults());
    private final WonderfulWolfGenomeProfile profile=new WonderfulWolfGenomeProfile(config,engine.geneSequenceCodec());
    private final WonderfulWolfFounderSynthesizer founder=new WonderfulWolfFounderSynthesizer(engine,profile);
    private final WonderfulWolfBreedingService breed=new WonderfulWolfBreedingService(engine,()->profile);

    private WonderfulWolfIndividual make(FounderGenomeSynthesis generated,FounderOrigin origin) {
        SynthesisResult.Success synthesis=assertInstanceOf(SynthesisResult.Success.class,generated.result());
        DiploidGenome genome=synthesis.genome();
        DecodeResult<WonderfulWolfDecodedPhenotype> canonical=engine.decode(profile,genome);
        PhenotypeSnapshot ph=canonical.phenotype().toSnapshot(canonical.identity(),origin==FounderOrigin.NATURAL?
           PhenotypeOrigin.NATURAL_FOUNDER:PhenotypeOrigin.WOLF_TRAP_FOUNDER);
        return new WonderfulWolfIndividual(genome,ph,Optional.empty(),0L,Mode.WANDER,Optional.empty(),
           ActionDistance.NORMAL,Optional.empty(),Map.of(),Optional.empty(),Map.of(),0,PedigreeSnapshot.founder());
    }
    private static List<String> traits(PhenotypeSnapshot ph) {
        return ph.expressedTraits().stream().map(t->t.trait().name()).toList();
    }

    private static String traitDetail(PhenotypeSnapshot ph) {
        return ph.expressedTraits().stream().map(t->t.trait().name()+"@"+t.strength().name())
            .reduce((a,b)->a+","+b).orElse("_");
    }
    private static boolean hasLocus(BitSequence sequence, MarkerLocus locus) {
        if (locus == null) return false;
        try {
            var method = MarkerEngine.class.getDeclaredMethod("resolveLocus",
                    BitSequence.class, MarkerLocus.class);
            method.setAccessible(true);
            return method.invoke(new MarkerEngine(), sequence, locus) != null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("MarkerEngine locus inspection failed", e);
        }
    }
    private static boolean inherited(String child, String a, String b) {
        return (a.indexOf(child.charAt(0)) >= 0 && b.indexOf(child.charAt(1)) >= 0)
            || (a.indexOf(child.charAt(1)) >= 0 && b.indexOf(child.charAt(0)) >= 0);
    }
    @Test void executeStatisticalCohort(){
        System.out.printf("STAT_META species=WWW shard=%d families=%d siblings_per_family=%d%n",
          GenomeStats.SHARD,GenomeStats.FAMILIES,GenomeStats.SIBLINGS);
        List<WonderfulWolfIndividual> parents=new ArrayList<>();
        for(int f=0;f<GenomeStats.FAMILIES;f++){
            for(int side=0;side<2;side++){
                final int family=f,s=side; long seed=GenomeStats.seed(f,1+side);
                FounderGenomeSynthesis generated=GenomeStats.timed("WWW","NATURAL",f,seed,
                  ()->founder.synthesize(FounderOrigin.NATURAL,seed));
                SynthesisResult.Success success=assertInstanceOf(SynthesisResult.Success.class,generated.result());
                parents.add(make(generated,FounderOrigin.NATURAL));
                System.out.printf("STAT_SUCCESS species=WWW stage=NATURAL shard=%d family=%d side=%d status=success%n",
                  GenomeStats.SHARD,family,s);
                if(side==0) GenomeStats.timed("WWW","DECODE",f,seed,()->engine.decode(profile,success.genome()));
            }
            long seed=GenomeStats.seed(f,90);
            FounderGenomeSynthesis trap=GenomeStats.timed("WWW","TRAP",f,seed,
              ()->founder.synthesize(FounderOrigin.WOLF_TRAP,seed));
            if(trap.result() instanceof SynthesisResult.Failure fail) {
                System.out.printf("STAT_FAILURE species=WWW stage=TRAP shard=%d family=%d reason=%s%n",
                  GenomeStats.SHARD,f,fail.reason());
            } else System.out.printf("STAT_SUCCESS species=WWW stage=TRAP shard=%d family=%d status=success%n",
                  GenomeStats.SHARD,f);
        }
        int successTotal=0;
        for(int f=0;f<GenomeStats.FAMILIES;f++) {
            WonderfulWolfIndividual a=parents.get(f*2),b=parents.get(f*2+1),
                 unrelated=parents.get((1-f)*2);
            for(int i=0;i<GenomeStats.SIBLINGS;i++){
                final int family=f,sibling=i;long seed=GenomeStats.seed(f,200+i);
                WonderfulWolfBreedingOutcome result=GenomeStats.timed("WWW","BREED",f,seed,
                  ()->breed.breed(new BreedingParent("A",a),new BreedingParent("B",b),Optional.empty(),seed));
                if(!(result instanceof WonderfulWolfBreedingOutcome.Success outcome)){
                    System.out.printf("STAT_FAILURE species=WWW stage=BREED shard=%d family=%d sibling=%d reason=%s%n",
                      GenomeStats.SHARD,f,i,result);
                    continue;
                }
                successTotal++;
                WonderfulWolfIndividual child=outcome.child();
                PhenotypeSnapshot c=child.phenotypeSnapshot(),ap=a.phenotypeSnapshot(),
                 bp=b.phenotypeSnapshot(),up=unrelated.phenotypeSnapshot();
                GenomeStats.genome("WWW",f,i,child.genome(),a.genome(),b.genome(),unrelated.genome());
                GenomeStats.phenotype("WWW",f,i,"ABILITY",ap.abilities(),bp.abilities(),c.abilities(),up.abilities());
                GenomeStats.phenotype("WWW",f,i,"PERSONALITY_FACTOR",ap.personalityFactors(),bp.personalityFactors(),
                  c.personalityFactors(),up.personalityFactors());
                GenomeStats.phenotype("WWW",f,i,"DEVELOPMENT",ap.developmentFactors(),bp.developmentFactors(),
                  c.developmentFactors(),up.developmentFactors());
                GenomeStats.categorical("WWW",f,i,ap.personality(),bp.personality(),c.personality(),up.personality(),
                  traits(ap),traits(bp),traits(c),traits(up));
                String markerA=engine.marker(profile.backbone(),a.genome()).formatted();
                String markerB=engine.marker(profile.backbone(),b.genome()).formatted();
                String markerC=engine.marker(profile.backbone(),child.genome()).formatted();
                String markerU=engine.marker(profile.backbone(),unrelated.genome()).formatted();
                assertEquals(markerC,outcome.lineageId());
                String[] ma=markerA.split("-"), mb=markerB.split("-"), mc=markerC.split("-");
                for (int chromosome=0; chromosome<mc.length; chromosome++) {
                    MarkerLocus locus=profile.backbone().chromosomes().get(chromosome).markerLocus();
                    ChromosomePair apair=a.genome().chromosomePairs().get(chromosome);
                    ChromosomePair bpair=b.genome().chromosomePairs().get(chromosome);
                    ChromosomePair cpair=child.genome().chromosomePairs().get(chromosome);
                    System.out.printf(Locale.ROOT,
                        "STAT_MARKER_LOCUS species=WWW shard=%d family=%d sibling=%d chr=%d reproducible=%s parentAresolved=%s,%s parentBresolved=%s,%s childResolved=%s,%s%n",
                        GenomeStats.SHARD,f,i,chromosome,inherited(mc[chromosome],ma[chromosome],mb[chromosome]),
                        hasLocus(apair.haplotypeA(),locus),hasLocus(apair.haplotypeB(),locus),
                        hasLocus(bpair.haplotypeA(),locus),hasLocus(bpair.haplotypeB(),locus),
                        hasLocus(cpair.haplotypeA(),locus),hasLocus(cpair.haplotypeB(),locus));
                }

                System.out.printf("STAT_MARKER species=WWW shard=%d family=%d sibling=%d parent_a=%s parent_b=%s child=%s unrelated=%s%n",
                  GenomeStats.SHARD,f,i,markerA,markerB,markerC,markerU);
                System.out.printf("STAT_TRAIT_DETAIL species=WWW shard=%d family=%d sibling=%d parent_a=%s parent_b=%s child=%s unrelated=%s%n",
                  GenomeStats.SHARD,f,i,traitDetail(ap),traitDetail(bp),traitDetail(c),traitDetail(up));

                System.out.printf(Locale.ROOT,
                  "STAT_GENETIC species=WWW shard=%d family=%d sibling=%d divine=%s injuries=%d initial_affection=%d affection_delta=%d%n",
                  GenomeStats.SHARD,f,i,c.divineLineageExpressed(),c.injuries().size(),
                  c.relationshipPerformance().initialAffection(),c.relationshipPerformance().affectionDelta());
            }
        }
        System.out.printf("STAT_COHORT_END species=WWW shard=%d breed_success=%d attempted=%d%n",
          GenomeStats.SHARD,successTotal,GenomeStats.FAMILIES*GenomeStats.SIBLINGS);
        assertTrue(successTotal>0);
    }
}
