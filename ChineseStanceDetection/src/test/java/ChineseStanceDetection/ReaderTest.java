package ChineseStanceDetection;

import static org.junit.Assert.assertEquals;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import ChineseStanceDetection.StanceDataReader;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationOutcome;

public class ReaderTest
{
    @Test
    public void combinationReaderTest() throws Exception {
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                StanceDataReader.class,
                StanceDataReader.PARAM_SOURCE_LOCATION, "sampleData.txt"
        );
        
        int i = 0;
        for (JCas jcas : new JCasIterable(reader)) {
            for (TextClassificationOutcome outcome : JCasUtil.select(jcas, TextClassificationOutcome.class)) {
                System.out.println(outcome.getCoveredText() + " - " + outcome.getOutcome());
            }
            i++;
        }
        
        assertEquals(2958, i);
    }
}