package ChineseStanceDetection;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.lab.Lab;
import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.ParameterSpace;
import org.dkpro.tc.api.features.TcFeatureFactory;
import org.dkpro.tc.api.features.TcFeatureSet;
import org.dkpro.tc.core.Constants;
import org.dkpro.tc.features.length.NrOfTokensPerSentence;
import org.dkpro.tc.features.twitter.EmoticonRatio;
import org.dkpro.tc.features.twitter.NumberOfHashTags;
import org.dkpro.tc.ml.ExperimentCrossValidation;
import org.dkpro.tc.ml.ExperimentTrainTest;
import org.dkpro.tc.ml.report.BatchCrossValidationReport;
import org.dkpro.tc.ml.report.BatchTrainTestReport;
import org.dkpro.tc.ml.weka.WekaClassificationAdapter;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import weka.classifiers.bayes.NaiveBayes;
//import weka.classifiers.functions.LinearRegression;
//import weka.classifiers.bayes.NaiveBayes;
//import weka.classifiers.functions.SMO;
//import de.tudarmstadt.ukp.dkpro.tc.features.length.NrOfTokensFeatureExtractor;
//import de.tudarmstadt.ukp.dkpro.tc.weka.writer.WekaDataWriter;

public class StanceDetectionTC
	implements Constants
{
public static final String LANGUAGE_CODE = "zh";

public static int NUM_FOLDS = 3;

//public static final String corpusFilePathTrain = "src/main/resources/train.txt";
//public static final String corpusFilePathTest = "src/main/resources/test.txt";

public static void main(String[] args)
        throws Exception
    {

        // This is used to ensure that the required DKPRO_HOME environment variable is set.
        // Ensures that people can run the experiments even if they haven't read the setup
        // instructions first :)
        // Don't use this in real experiments! Read the documentation and set DKPRO_HOME as
        // explained there.
//        DemoUtils.setDkproHome(WekaTwitterSentimentDemo.class.getSimpleName());
    	System.setProperty("DKPRO_HOME", System.getProperty("user.home")+"/Desktop/");

        ParameterSpace pSpace = getParameterSpace();

        StanceDetectionTC experiment = new StanceDetectionTC();
//        experiment.runCrossValidation(pSpace);
        experiment.runTrainTest(pSpace);
    }

    @SuppressWarnings("unchecked")
    public static ParameterSpace getParameterSpace()
        throws ResourceInitializationException
    {
        // configure training and test data reader dimension
        // train/test will use both, while cross-validation will only use the train part
        // The reader is also responsible for setting the labels/outcome on all
        // documents/instances it creates.
        Map<String, Object> dimReaders = new HashMap<String, Object>();

        CollectionReaderDescription readerTrain = CollectionReaderFactory.createReaderDescription(
                StanceDataReader.class, StanceDataReader.PARAM_SOURCE_LOCATION,
                "src/main/resources/train.txt", StanceDataReader.PARAM_LANGUAGE, LANGUAGE_CODE);
        dimReaders.put(DIM_READER_TRAIN, readerTrain);

        CollectionReaderDescription readerTest = CollectionReaderFactory.createReaderDescription(
        		StanceDataReader.class, StanceDataReader.PARAM_SOURCE_LOCATION,
        		"src/main/resources/test.txt", StanceDataReader.PARAM_LANGUAGE, LANGUAGE_CODE);
        dimReaders.put(DIM_READER_TEST, readerTest);

        Dimension<List<String>> dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
                Arrays.asList(new String[] { NaiveBayes.class.getName() }));

        
        Dimension<TcFeatureSet> dimFeatureSets = Dimension.create(
                DIM_FEATURE_SET,
                new TcFeatureSet(TcFeatureFactory.create(NrOfTokensPerSentence.class),
                        TcFeatureFactory.create(EmoticonRatio.class),
                        TcFeatureFactory.create(NumberOfHashTags.class)));
        
        ParameterSpace pSpace = new ParameterSpace(Dimension.createBundle("readers", dimReaders),
                Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL),
                Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT), dimFeatureSets,
                dimClassificationArgs);

        return pSpace;
    }

    // ##### CV #####
    protected void runCrossValidation(ParameterSpace pSpace)
        throws Exception
    {
        ExperimentCrossValidation batch = new ExperimentCrossValidation("StanceCV",
                WekaClassificationAdapter.class, NUM_FOLDS);
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.addReport(BatchCrossValidationReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    // ##### TRAIN-TEST #####
    protected void runTrainTest(ParameterSpace pSpace)
        throws Exception
    {
        ExperimentTrainTest batch = new ExperimentTrainTest("Stance",
                WekaClassificationAdapter.class);
        batch.setPreprocessing(getPreprocessing());
        batch.setParameterSpace(pSpace);
        batch.addReport(BatchTrainTestReport.class);

        // Run
        Lab.getInstance().run(batch);
    }

    protected AnalysisEngineDescription getPreprocessing()
        throws ResourceInitializationException
    {
        return createEngineDescription(
        		createEngineDescription(LanguageToolSegmenter.class)
        );
    }
}