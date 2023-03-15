// Luca 7: change Channel names and colors
/*
 * C1    DAPI        Nuclei    white
 * C2    Opal480     CD8       cyan
 * C3    Opal520     PDL1      green
 * C4    Opal570     Ki67      yellow
 * C5    Opal620     CD68      orange
 * C6    Opal690     PanCK     red
 * C7    Opal780     CD20      magenta
 * C8    AF          AF        black
 */
setImageType('FLUORESCENCE')

// convert original channel names from DAPI, FITC, CY3, Texas Red, CY5 
setChannelNames('Nuclei', 'CD8', 'PDL1', 'Ki67', 'CD68', 'PanCK', 'CD20', 'AF')

// Set Colors to white, cyan, green, yellow, orange, red, magenta, black
setChannelColors(
    getColorRGB(255, 255, 255),
    getColorRGB(0, 188, 227),
    getColorRGB(0, 255, 0),
    getColorRGB(255, 255, 0),
    getColorRGB(255, 179, 66), 
    getColorRGB(255, 0, 0),
    getColorRGB(255, 0, 255),
    getColorRGB(0, 0, 0)
)

// Reset display range
setChannelDisplayRange('Nuclei',0, 290)
setChannelDisplayRange('CD8',0, 30)
setChannelDisplayRange('PDL1',0, 20)
setChannelDisplayRange('Ki67',0, 25)
setChannelDisplayRange('CD68',0, 7)
setChannelDisplayRange('PanCK',0, 30)
setChannelDisplayRange('CD20',0, 15)
setChannelDisplayRange('AF',0, 350)


