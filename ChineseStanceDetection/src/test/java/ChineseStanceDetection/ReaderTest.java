package ChineseStanceDetection;

import static org.junit.Assert.*;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.tc.api.type.TextClassificationOutcome;
import org.junit.Test;

public class ReaderTest
{
    @Test
    public void combinationReaderTest() throws Exception {
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                StanceDataReader.class,
                StanceDataReader.PARAM_SOURCE_LOCATION, "src/main/resources/train.txt"
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