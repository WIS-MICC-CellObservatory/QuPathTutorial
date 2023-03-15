
// Script for Full Analysis of Tonsil Project 
// Used for QuPath Basics Tutorial by Ofra Golani, MICC Cell Observatory
//
// Make sure you applied the 
import qupath.ext.stardist.StarDist2D

// Script Operation Parameters
def UseFullImageAnnotationInsteadOfSelectedClass = 1    // Keep 0 for automated selection of ROI for analysis using classifier or for using the manually drawn annotation.
                                                        // Set to 1 if you want to automaticall select the whole image for analysis 
                                                        // Note that you should set it to the same Class you selected for ClassToSelect
                                                        
def RunWholeTissueClassifier = 1                        // Keep 1 for Automatic selection of region of interest using a Classifier
                                                        // Set to 0 if you want to draw the Region of interest selection on all the images.
def deleteExistingObjects = 1                           // keep 1, Set it to 0 for special cases where you want to keep existing 
def runStarDist = 1
def filterSomeCells = 1
def classifyCells = 1

def RunPixelClassifier        = 1
def runAnnotationExpansion    = 1
def calcDistanceToAnnotations = 1
def exportLabelImages         = 1

// Parameters for Whole Tissue Selection
def ClassToSelect            = "WholeTissue"           // Name of selected Class
def WholeTissueClassifierName = "WholeTissueFinder"     // Name of classifier
def MinWholeTissueSize       = 200000 // um^2
def MinWholeTissueHolesize   = 10000 // um^2

// StarDist based Cell Segmentation parameters 

// Specify the model .pb file (you will need to change this!)
//def pathModel = '/path/to/dsb2018_heavy_augment.pb'
def pathModel = 'A:/shared/QuPathScriptsAndProtocols/QuPath_StarDistModels/dsb2018_heavy_augment.pb'
//def pathModel = 'Z:/UserData/shared/QuPathScriptsAndProtocols/QuPath_StarDistModels/stardist_for_vishnu_v5.pb'

def NucChannel = 'Nuclei'
def ProbabilityThreshold = 0.5
def PixelSize            = 0.2485  // Resolution for detection, make sure to set it to the Pixel Size of your data
def CellExpansion        = 5.0 // pixels 
//def CellExpansion      = 0

// Further cell filtering parameters
def MaxNucArea        = 300   // um^2      
def MinNucArea        = 10    // um^2
def MinNucIntensity   = 20      // remove any detections with an intensity less than or equal to this value

// Cell Classification Parameters
def CellClassifierName = 'CD8_Ki67_CD20_ML_v1'

// Pixel Classifier Parameters 
def PixelClassifier = "FollicleEpithelialFinder"
def Minimum_ObjectSize = 10000
def Minimum_LumenSize  = 2000

// Annotation expanstion parameters
def AnnotationClassToExpand = "Follicle"
def ExpandRadius_um = 50.0 

// Results parameters  
def ResultsSubFolder = 'export'
def downsample     = 1 // 10
def labelsSubFolder = "image_export" 
def outputSuffixCellLabels        = "_CellLabels.tif"
def outputSuffixAnnotationsLabels = "_AnnotationLabels.tif"


// ================== END OF USER DEFINED PARAMETERS ================

def stardist = StarDist2D.builder(pathModel)
        .threshold(ProbabilityThreshold) // Probability (detection) threshold
        .channels(NucChannel)            // Select detection channel
        .normalizePercentiles(1, 99)     // Percentile normalization
        .pixelSize(PixelSize)            // Resolution for detection
        .cellExpansion(CellExpansion)    // Approximate cells based upon nucleus expansion
        .cellConstrainScale(1.5)         // Constrain cell expansion using nucleus size
        .measureShape()                  // Add shape measurements
        .measureIntensity()              // Add cell measurements (in all compartments)
        .includeProbability(true)        // Add probability as a measurement (enables later filtering)
        .build()

// ============= MAIN CODE =======================

println '==========================================================='
// Run detection for the selected objects
def imageData = getCurrentImageData()
// Get name of current image    
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())

if (imageData.ImageType == ImageData.ImageType.FLUORESCENCE) 
{
   
    // Select WholeTissue Annotation
    //selectObjectsByClassification(SelectedObjectClass);
    resetSelection()
    if (UseFullImageAnnotationInsteadOfSelectedClass ) 
    {
       if (deleteExistingObjects)
       {
            clearAllObjects();           
       }
       createFullImageAnnotation(true)
       pathObjects = getSelectedObjects()
       def classification = getPathClass(ClassToSelect)
       pathObjects.each{it.setPathClass(classification)}
    }
    else 
    {
        if (RunWholeTissueClassifier)   
        {
            if (deleteExistingObjects)
            {
                createAnnotationsFromPixelClassifier(WholeTissueClassifierName, MinWholeTissueSize, MinWholeTissueHolesize, "DELETE_EXISTING", "SELECT_NEW")
            }
            else
            {
                createAnnotationsFromPixelClassifier(WholeTissueClassifierName, MinWholeTissueSize, MinWholeTissueHolesize, "SELECT_NEW")
            }
        }
        else
        {
            selectObjectsByClassification(ClassToSelect);                    
        }
    }
    
    //def pathObjects = getSelectedObjects()
    pathObjects = getSelectedObjects();
    //def pathObjects = getAnnotationObjects().findAll{it.getPathClass() != getPathClass("WholeTissue")}
    //def pathObjects = getAnnotationObjects().findAll{it.getPathClass() != getPathClass(ClassToSelect)}
    
    if (pathObjects.isEmpty()) 
    {
        //print("Image: " + name + " - No Annotation of Class " + SelectedObjectClass + " found");
        print("Image: " + name + " - No Annotation of Class WholeTissue found");
        //continue;
        //Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
        //return
    } else 
    {
        print("Processing Image: " + name );
        
        if (runStarDist == 1) 
        {
            println '============== Running StarDist... =================='
            stardist.detectObjects(imageData, pathObjects)
            
            if (filterSomeCells == 1)
            {
                println '============== Filter Out small / big / deem Cells ... =================='
                // Filter Nuc by size and Intensity
                NucAreaMeasurement='Nucleus: Area µm^2' //Name of the measurement you want to perform filtering on
                if (CellExpansion == 0) 
                {
                    NucAreaMeasurement='Area µm^2' //Name of the measurement you want to perform filtering on
                }
                toDelete =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) > MaxNucArea}
                removeObjects(toDelete, true)
                toDelete1 =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) < MinNucArea}
                removeObjects(toDelete1, true)
                
                NucIntensityMeasurement='Nuclei: Nucleus: Mean' //Name of the measurement you want to perform filtering on
                if (CellExpansion == 0)
                {
                    NucIntensityMeasurement='Nuclei: Mean' //Name of the measurement you want to perform filtering on
                }
                toDelete2 = getDetectionObjects().findAll {measurement(it, NucIntensityMeasurement) <= MinNucIntensity}
                removeObjects(toDelete2, true)
            }                
                      
            // Classify Cells 
            if (classifyCells == 1)
            {
                println '============== Classfying Cells ... =================='            
                resetDetectionClassifications();
                cells = getCellObjects()
                classifier = loadObjectClassifier(CellClassifierName)
                classifier.classifyObjects(imageData, cells, false)
                fireHierarchyUpdate()
            }
        } // if runStarDist
        
        if (RunPixelClassifier == 1)
        {
            // Create Pixel-Classifier-based objects
            println '============== Run Pixel Classifier ... =================='            
            resetSelection()
            selectObjectsByClassification("WholeTissue");
            createAnnotationsFromPixelClassifier(PixelClassifier, Minimum_ObjectSize, Minimum_LumenSize, "SPLIT")
            pathObjects = getSelectedObjects();
            //selectAnnotations();
            //addPixelClassifierMeasurements(PixelClassifier, PixelClassifier)
        }            
               
        if (runAnnotationExpansion == 1)
        {
            println '============== Expand Annotation ... =================='                   
            clearSelectedObjects();
            //selectAnnotations();
            AnnotationsToExpand = getAnnotationObjects().findAll{(it.getPathClass() == getPathClass(AnnotationClassToExpand)) }
            
            // Create a Ring around each annotation 
            selectObjects(AnnotationsToExpand )
            runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+ExpandRadius_um+',  "lineCap": "Round",  "removeInterior": true,  "constrainToParent": true}');
            //runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": '+expand_radius2_um+',  "lineCap": "Round",  "removeInterior": true,  "constrainToParent": true}');                                
        }
        fireHierarchyUpdate()

        if (calcDistanceToAnnotations)
        {
            detectionToAnnotationDistancesSigned(false)
        }

        // save annotations
        //File directory = new File(buildFilePath(PROJECT_BASE_DIR,'export'));
        File directory = new File(buildFilePath(PROJECT_BASE_DIR,ResultsSubFolder));
        directory.mkdirs();
        //imageName = ServerTools.getDisplayableImageName(imageData.getServer())
        imageName = GeneralTools.getNameWithoutExtension(getCurrentImageData().getServer().getMetadata().getName())
        saveAnnotationMeasurements(buildFilePath(directory.toString(),imageName+'_annotations.csv'));
        saveDetectionMeasurements(buildFilePath(directory.toString(),imageName+'_detections.csv'));
        
        if (exportLabelImages)
        {
            //def imageData = getCurrentImageData()
            //Make sure the location you want to save the files to exists - requires a Project
            
            def pathOutput = buildFilePath(PROJECT_BASE_DIR, labelsSubFolder)
            mkdirs(pathOutput)
            
            def cellLabelServer = new LabeledImageServer.Builder(imageData)
                .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
                //.useCells()
                .useDetections()
                .useInstanceLabels()
                .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported    
                .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
                .build()
            
            def annotationLabelServer = new LabeledImageServer.Builder(imageData)
                .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
                .useAnnotations()
                .useInstanceLabels()
                .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported    
                .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
                .build()
            
            // Write the full image downsampled by a factor of downsample
            def requestFull = RegionRequest.createInstance(getCurrentServer(), downsample)
            pathOutput = buildFilePath(PROJECT_BASE_DIR, labelsSubFolder, imageName)
            //Now to export one image of each type per annotation (in the default case, unclassified)
            
            //objects with overlays as seen in the Viewer    
            //writeRenderedImageRegion(getCurrentViewer(), requestROI, pathOutput+"_rendered.tif")
            //Labeled images, either cells or annotations
            writeImageRegion(annotationLabelServer, requestFull, pathOutput+outputSuffixAnnotationsLabels)   // Annotations labels
            writeImageRegion(cellLabelServer, requestFull, pathOutput+outputSuffixCellLabels)                // Cell labels
                        
        }
    } // if not empty list of selectedClass            
} // if FLUORESCENCE
else
{
    print("Image: " + name + " - Not FLUORESCENCE ");
}
print("=============== Image: " + name + " Done ========================");
//println '====================== Done! ======================='

