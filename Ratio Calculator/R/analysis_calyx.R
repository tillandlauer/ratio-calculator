####################################################
# ANALYSIS OF RATIO DATA (CALYX VERSION)           #
# v1.1, 17-06-11, Till Andlauer, till@andlauer.net #
####################################################
# setwd("/Users/till/Documents/ownCloud/GWDG/Misc/Ratios/Ratio\ Analysis")

library(ggplot2)
source("ratioFunctions.R")

ratios <- read.table("ratios.txt",h=T,sep="\t") # ratios conversion table
dDir <- "data/calyx/" # end path with "/", otherwise subsequent functions won't work
dir.create(paste0(dDir,"/plots"),showWarnings=F) # create plot directory
dir.create(paste0(dDir,"/results"),showWarnings=F) # create results directory

##########################################
# Compare differences between situations #
##########################################
# weighted quantiles ratio are calculated by counting the normalized frequencies per file
# the GAL4/GFP ratios are normalized by the reference neuropil 
# therefore the results are GFP ratios relative to the reference neuropil ratios
# each numeric, relative/normalized value needs to be interpreted on its own
# because the values are relative to the reference, the 25% quantile can have a higher value than the 75% quantile
# the normalized min/max quantiles cannot be interpreted and are therfore omitted
# each quantile can thus only be analyzed by itself and it is recommended to only analyze the normalized medians

# load ratio data and calculate normalized quantile ratios
gal4s <- c("17D","GAD-Gal80","Mz19") # names
glist <- genQuant(dDir,gal4s,ratios) # calculate normalized quantile ratios (returned as lists)
saveRDS(glist,"gal4_lines_calyx.RDS") # save data for future analyses
# glist <- readRDS("gal4_lines_calyx.RDS") # load previously generated ratio data

###############################################################
# Compare all normalized median antibody ratios per GAL4 line #
###############################################################
# Mann-Whitney U test
sink(paste0(dDir,"/results/mwu_tests_antibodies_calyx.txt"),split=T) # save output to a file
mwu_gal4ratios(glist) # This analysis tests whether antibody ratios differ within a GAL4 line
sink(NULL) # end writing to the file

# For comparison: Dunn's test
dunn_gal4ratios(glist) # This analysis tests whether antibody ratios differ within a GAL4 line

###################################################################
# Test whether the normalized median rations are different from 1 #
###################################################################
# Uses both Mann-Whitney U test and Student's t test
sink(paste0(dDir,"/results/mwu_tests_antibodies_different_calyx.txt"),split=T) # save output to a file
suppressWarnings(median_diff(glist)) # suppressWarnings is used because of MWU warnings in case of ties
sink(NULL) # end writing to the file

######################################################
# Plot median ratios of antibodies per GAL4/GFP line #
######################################################
gal4s <- c("17D","GAD-Gal80","Mz19")

boxplot_gal4(dDir, gal4s, glist)

##################################################################################
# Compare normalized median ratios between all GAL4 lines per antibody: MWU test #
##################################################################################
abList <- c("BrpNT","RBP","Syd1")

sink(paste0(dDir,"/results/mwu_tests_gal4_calyx.txt"),split=T) # save output to a file
lastResult <- mwu_ABratios(abList, glist)
sink(NULL) # end writing to the file

#############################################################
# Plot quantiles and medians of GAL4/GFP lines per antibody #
#############################################################
boxplot_ab(dDir, abList, glist)

###################################################################################
# Generate histograms, violin plots, and subtracted histograms for all GAL4 lines #
###################################################################################
gal4s <- c("17D","GAD-Gal80","Mz19") # names

for (j in seq(1,length(gal4s)))
  {
  print(paste0("Plotting ",gal4s[j],"..."))
  files <- listFiles(dDir,gal4s[j]) # count the number of measurements per GAL4 line
  for (i in seq(1,length(files)))
    {
    ab <- colnames(files)[i] # extract antibody used for measurements
    numFiles <- files[[i]] # number of files
    plotBoth(dDir,gal4s[j],ab,numFiles,ratios) # plot histograms and violin plots
    plotDiff(dDir,gal4s[j],ab,numFiles,ratios) # subtract the reference neuropil from the GAL4/GFP histogram
    }
  }


################
### APPENDIX ###
################

########################################################
# Example: Compare one antibody between two GAL4 lines #
########################################################
# This example illustrates how data is analyzed and plotted in the subsequent functions

# compare normalized median ratios of two GAL4 lines
selGAL4 <- c("17D","Mz19")
selAB <- c("Syd1")
wilcox.test(glist[[selGAL4[1]]][[selAB]]$median,glist[[selGAL4[2]]][[selAB]]$median)

# generate boxplot only using the median ratios
# note that the number of measurements can differ between GAL4 lines
pData <- data.frame(cbind(selGAL4[1],glist[[selGAL4[1]]][[selAB]]$median))
pData <- rbind(pData,data.frame(cbind(selGAL4[2],glist[[selGAL4[2]]][[selAB]]$median)))
colnames(pData) <- c("GAL4","median")
pData$median <- as.numeric(as.character(pData$median))

ggplot(pData, aes(x=GAL4,y=median)) +geom_boxplot(aes(fill=GAL4),size=I(1.5)) +
  scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio ",selAB,"/BrpCT")) +
  theme_bw() +theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"))

#######
# END #
#######
