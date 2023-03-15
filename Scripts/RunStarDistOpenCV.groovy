import qupath.ext.stardist.StarDist2D

// Specify the model .pb file (you will need to change this!)
var pathModel = 'A:/shared/QuPathScriptsAndProtocols/QuPath_StarDistModels/dsb2018_heavy_augment.pb'

def NucChannel = 'Nuclei' // 
def ProbabilityThreshold = 0.5
def PixelSize = 0.2485  // 0.5  // Resolution for detection
def CellExpansion = 5 //0

// Further object filtering parameters
def MaxNucArea= 2000 //350 
def MinNucArea= 0 //20

//def MaxNucArea= 20000
//def MinNucArdea= 0

//def MinNucIntensity=250 //remove any detections with an intensity less than or equal to this value
def MinNucIntensity=0 //120 //remove any detections with an intensity less than or equal to this value

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

// Run detection for the selected objects
def imageData = getCurrentImageData()
def pathObjects = getSelectedObjects()
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
    return
}


println '============== Running StarDist... =================='
stardist.detectObjects(imageData, pathObjects)

// Filter Nuc by size and Intensity
def NucAreaMeasurement='Nucleus: Area µm^2' //Name of the measurement you want to perform filtering on
if (CellExpansion == 0) 
{
    NucAreaMeasurement='Area µm^2' //Name of the measurement you want to perform filtering on
}
def toDelete =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) > MaxNucArea}
removeObjects(toDelete, true)
def toDelete1 =  getDetectionObjects().findAll {measurement(it, NucAreaMeasurement) < MinNucArea}
removeObjects(toDelete1, true)

def NucIntensityMeasurement='Nuclei: Nucleus: Mean' //Name of the measurement you want to perform filtering on
if (CellExpansion == 0)
{
    NucIntensityMeasurement='Nuclei: Mean' //Name of the measurement you want to perform filtering on
}
def toDelete2 = getDetectionObjects().findAll {measurement(it, NucIntensityMeasurement) <= MinNucIntensity}
removeObjects(toDelete2, true)

   
println '============== Done! =================='
