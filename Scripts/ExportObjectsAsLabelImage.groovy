// ExportObjectsAsLabeleImage: 
// Create 2 labeled images under outSubFolder - one for detections and one for annotations 

// 1 is full resolution. You may want something more like 20 or higher for small thumbnails
def downsample     = 1 // 10
def ClassToSelect  = "WholeTissue"           // Name of selected Class of the whole Tissue

def outSubFolder = "image_export" 
def outputSuffixCellLabels        = "_CellLabels.tif"
def outputSuffixAnnotationsLabels = "_AnnotationLabels.tif"

//remove the findAll to get all annotations, or change the null to getPathClass("Tumor") to only export Tumor annotations
//annotations = getAnnotationObjects().findAll{it.getPathClass() == null}
//annotations = getAnnotationObjects().findAll{it.getPathClass() == getPathClass(ClassToSelect)}

//def imageNameWithExt = getCurrentImageData().getServer().getMetadata().getName()
def imageName = GeneralTools.getNameWithoutExtension(getCurrentImageData().getServer().getMetadata().getName())
def imageData = getCurrentImageData()
//Make sure the location you want to save the files to exists - requires a Project

def pathOutput = buildFilePath(PROJECT_BASE_DIR, outSubFolder)
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
pathOutput = buildFilePath(PROJECT_BASE_DIR, outSubFolder, imageName)
//Now to export one image of each type per annotation (in the default case, unclassified)

//objects with overlays as seen in the Viewer    
//writeRenderedImageRegion(getCurrentViewer(), requestROI, pathOutput+"_rendered.tif")
//Labeled images, either cells or annotations
writeImageRegion(annotationLabelServer, requestFull, pathOutput+outputSuffixAnnotationsLabels)   // Annotations labels
writeImageRegion(cellLabelServer, requestFull, pathOutput+outputSuffixCellLabels)                // Cell labels


println 'Done, image saved in '+ pathOutput
