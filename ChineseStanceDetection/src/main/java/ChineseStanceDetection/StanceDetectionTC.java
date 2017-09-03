package ChineseStanceDetection;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.log4j.BasicConfigurator;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;

import ChineseStanceDetection.StanceDataReader;

import weka.classifiers.functions.LibSVM;
//import weka.classifiers.functions.LinearRegression;
//import weka.classifiers.bayes.NaiveBayes;
//import weka.classifiers.functions.SMO;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
//import de.tudarmstadt.ukp.dkpro.tc.features.length.NrOfTokensFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.base.NGramFeatureExtractorBase;
import de.tudarmstadt.ukp.dkpro.tc.weka.report.BatchCrossValidationReport;
import de.tudarmstadt.ukp.dkpro.tc.weka.report.BatchOutcomeIDReport;
import de.tudarmstadt.ukp.dkpro.tc.weka.report.BatchTrainTestReport;
import de.tudarmstadt.ukp.dkpro.tc.weka.report.ClassificationReport;
import de.tudarmstadt.ukp.dkpro.tc.weka.task.BatchTaskCrossValidation;
import de.tudarmstadt.ukp.dkpro.tc.weka.task.BatchTaskTrainTest;
//import de.tudarmstadt.ukp.dkpro.tc.weka.writer.WekaDataWriter;

public class StanceDetectionTC
implements Constants
{
public static final String LANGUAGE_CODE = "zh";

public static int NUM_FOLDS = 3;

public static final String corpusFilePathTrain = "src/main/resources/TrainingData.txt";
public static final String corpusFilePathTest = "src/main/resources/TestData.txt";

public static void main(String[] args)
    throws Exception
{
	//BasicConfigurator.configure();
    ParameterSpace pSpace = getParameterSpace();

    StanceDetectionTC experiment = new StanceDetectionTC();
    //experiment.runCrossValidation(pSpace);
    
    // enable when test data is available
    experiment.runTrainTest(pSpace);
}

public static ParameterSpace getParameterSpace()
{
    // configure training and test data reader dimension
    Map<String, Object> dimReaders = new HashMap<String, Object>();
    dimReaders.put(DIM_READER_TRAIN, StanceDataReader.class);
    dimReaders.put(DIM_READER_TRAIN_PARAMS, Arrays.asList(new Object[] {
    		StanceDataReader.PARAM_SOURCE_LOCATION, corpusFilePathTrain,
    		StanceDataReader.PARAM_LANGUAGE, LANGUAGE_CODE }
    ));
    dimReaders.put(DIM_READER_TEST, StanceDataReader.class);
    dimReaders.put(DIM_READER_TEST_PARAMS, Arrays.asList(new Object[] {
    		StanceDataReader.PARAM_SOURCE_LOCATION, corpusFilePathTest,
    		StanceDataReader.PARAM_LANGUAGE, LANGUAGE_CODE}
    ));

    // lets try svm classifier from Weka
    @SuppressWarnings("unchecked")
    Dimension<List<String>> dimClassificationArgs = Dimension.create(
            DIM_CLASSIFICATION_ARGS,
            Arrays.asList(new String[] { LibSVM.class.getName() })
    );

    // in this example, we only use two feature extractors
    // - length in tokens
    // - frequent n-grams
    // - emoticon counts
    // a real experiment would use many more
    // 
    // you can use pre-defined ones from
    // http://code.google.com/p/dkpro-tc/
    // or write your own extractors
    @SuppressWarnings("unchecked")
    Dimension<List<String>> dimFeatureSets = Dimension.create(
            DIM_FEATURE_SET,
            /*Arrays.asList(new String[] {
                    NrOfTokensFeatureExtractor.class.getName()
                    
            }),
            Arrays.asList(new String[] {
                    NrOfTokensFeatureExtractor.class.getName(),
                    NGramFeatureExtractor.class.getName() 
            }),
            Arrays.asList(new String[] {
                    NrOfTokensFeatureExtractor.class.getName(),
                    NGramFeatureExtractor.class.getName(),
                    EmoticonFeatureExtractor.class.getName()                     
            })
*/          Arrays.asList(new String[] {
				    NGramFeatureExtractorBase.class.getName(),                   
            })
    );
    
    // configuring feature extractors
    // 
    // each group represents one run with the corresponding parameters
    @SuppressWarnings("unchecked")
    Dimension<List<Object>> dimPipelineParameters = Dimension.create(
            DIM_PIPELINE_PARAMS,
            Arrays.asList(new Object[] {
            		NGramFeatureExtractorBase.PARAM_NGRAM_USE_TOP_K, "50",
            		NGramFeatureExtractorBase.PARAM_NGRAM_MIN_N, 1,
            		NGramFeatureExtractorBase.PARAM_NGRAM_MAX_N, 3
            }),
            Arrays.asList(new Object[] {
            		NGramFeatureExtractorBase.PARAM_NGRAM_USE_TOP_K, "100",
            		NGramFeatureExtractorBase.PARAM_NGRAM_MIN_N, 1,
            		NGramFeatureExtractorBase.PARAM_NGRAM_MAX_N, 3
            })
    );

    @SuppressWarnings("unchecked")
    ParameterSpace pSpace = new ParameterSpace(
            Dimension.createBundle("readers", dimReaders),
            //Dimension.create(DIM_DATA_WRITER, WekaDataWriter.class.getName()),
            //Dimension.create(DIM_MULTI_LABEL, false),
            Dimension.create(DIM_FEATURE_MODE, LM_SINGLE_LABEL),
            Dimension.create(DIM_FEATURE_MODE, FM_DOCUMENT),
            dimPipelineParameters,
            dimFeatureSets,
            dimClassificationArgs
    );

    return pSpace;
}

// ##### CV #####
protected void runCrossValidation(ParameterSpace pSpace)
    throws Exception
{

    BatchTaskCrossValidation batch = new BatchTaskCrossValidation("SemEval2014_Task9_Subtask_B_CV",
            getPreprocessing(), NUM_FOLDS);
    batch.addInnerReport(ClassificationReport.class);
    batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
    batch.addReport(BatchCrossValidationReport.class);

    // Run
    Lab.getInstance().run(batch);
}

// ##### TRAIN-TEST #####
protected void runTrainTest(ParameterSpace pSpace)
    throws Exception
{

    BatchTaskTrainTest batch = new BatchTaskTrainTest("SemEval2014_Task9_Subtask_B_TrainTest",
            getPreprocessing());
    batch.addInnerReport(ClassificationReport.class);
    batch.setParameterSpace(pSpace);
    batch.setExecutionPolicy(ExecutionPolicy.RUN_AGAIN);
    batch.addReport(BatchTrainTestReport.class);
    batch.addReport(BatchOutcomeIDReport.class);

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
