package ChineseStanceDetection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationOutcome;
// import de.tudarmstadt.ukp.dkpro.tc.examples.single.document.WekaSimpleDkproTCReaderDemo;

/**
 * A very basic DKPro TC reader, which reads sentences from a text file and labels from another text
 * file. It is used in {@link WekaSimpleDkproTCReaderDemo}.
 * 
 */
public class StanceDataReader
    extends AbstractReader

{

    // item list one entry for each data. each entry contains all stance for the text 
 	List<String[]> items;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        
        items = new ArrayList<String[]>();
        
        for (String line:rawData) {
        		String[] item = line.split(separator);
        		if(item.length != 4){
        			throw new ResourceInitializationException("Parsing error. Line must have 4 segments.", item);
        		}
        		items.add(item);
        }             

        currentIndex = 0;
    }

    public boolean hasNext()
        throws IOException, CollectionException
    {
        return currentIndex < rawData.size();
    }

    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        // <ID><TARGET><TEXT><STANCE>
        String[] item = items.get(currentIndex);
        
        String id = item[0];
        String target = item[1];
        String text = item[2];
        String stance = item[3];
        
        aJCas.setDocumentText(text);
        aJCas.setDocumentLanguage(language);

        // as we are creating more than one CAS out of a single file, we need to have different
        // document titles and URIs for each CAS
        // otherwise, serialized CASes will be overwritten
        DocumentMetaData dmd = DocumentMetaData.create(aJCas);
        dmd.setDocumentTitle(new Integer(currentIndex).toString());
        dmd.setDocumentUri(target + "#" + id);
        dmd.setDocumentId(corpusFile);

        // setting the outcome / label for this document
        TextClassificationOutcome outcome = new TextClassificationOutcome(aJCas);
        outcome.setOutcome(stance);
        outcome.addToIndexes();
        currentIndex++;
    }

    public Progress[] getProgress()
    {
        return new Progress[] { new ProgressImpl(currentIndex, rawData.size(), Progress.ENTITIES) };
    }
}