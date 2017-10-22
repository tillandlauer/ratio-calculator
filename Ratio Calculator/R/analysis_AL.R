####################################################
# ANALYSIS OF RATIO DATA (ANTENNAL LOBE VERSION)   #
# v1.1, 17-06-11, Till Andlauer, till@andlauer.net #
####################################################
# setwd("/Users/till/Documents/ownCloud/GWDG/Misc/Ratios/Ratio\ Analysis")

library(ggplot2)
source("ratioFunctions.R")

ratios <- read.table("ratios.txt",h=T,sep="\t") # ratios conversion table
dDir <- "data/AL/" # end path with "/", otherwise subsequent functions won't work
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
gal4s <- c("OR83B","OR46A","OR47B","OR67D_DA1","OR67D_VA6","NP1227","Krasa","Mz19")
glist <- genQuant(dDir,gal4s,ratios) # calculate normalized quantile ratios (returned as lists)
saveRDS(glist,"gal4_lines.RDS") # save data for future analyses
glist <- readRDS("gal4_lines.RDS") # load previously generated ratio data

###############################################################
# Compare all normalized median antibody ratios per GAL4 line #
###############################################################
# Mann-Whitney U test
sink(paste0(dDir,"/results/mwu_tests_antibodies.txt"),split=T) # save output to a file
mwu_gal4ratios(glist) # This analysis tests whether antibody ratios differ within a GAL4 line
sink(NULL) # end writing to the file

# For comparison: Dunn's test
dunn_gal4ratios(glist) # This analysis tests whether antibody ratios differ within a GAL4 line

###################################################################
# Test whether the normalized median rations are different from 1 #
###################################################################
# Uses both Mann-Whitney U test and Student's t test
sink(paste0(dDir,"/results/mwu_tests_antibodies_different.txt"),split=T) # save output to a file
suppressWarnings(median_diff(glist)) # suppressWarnings is used because of MWU warnings in case of ties
sink(NULL) # end writing to the file

######################################################
# Plot median ratios of antibodies per GAL4/GFP line #
######################################################
gal4s <- c("OR83B","OR46A","OR47B","OR67D_DA1","OR67D_VA6","NP1227","Krasa","Mz19")

boxplot_gal4(dDir, gal4s, glist)

##################################################################################
# Compare normalized median ratios between all GAL4 lines per antibody: MWU test #
##################################################################################
abList <- c("BrpNT","RBP","Syd1")

sink(paste0(dDir,"/results/mwu_tests_gal4.txt"),split=T) # save output to a file
mwu_ABratios(abList, glist)
sink(NULL) # end writing to the file

#############################################################
# Plot quantiles and medians of GAL4/GFP lines per antibody #
#############################################################
boxplot_ab(dDir, abList, glist)

###################################################################################
# Generate histograms, violin plots, and subtracted histograms for all GAL4 lines #
###################################################################################
gal4s <- c("OR83B","OR46A","OR47B","OR67D_DA1","OR67D_VA6","NP1227","Krasa","Mz19") # names

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

############################################
# Generate all plots for single GAL4 lines #
############################################
gal4 <- "OR65A_incomplete"
files <- listFiles(dDir,gal4)
for (i in seq(1,length(files)))
  {
  ab <- colnames(files)[i]
  numFiles <- files[[i]]
  plotBoth(dDir,gal4,ab,numFiles,ratios)
  plotDiff(dDir,gal4,ab,numFiles,ratios)
  }


################
### APPENDIX ###
################

########################################################
# Example: Compare one antibody between two GAL4 lines #
########################################################
# This example illustrates how data is analyzed and plotted in the subsequent functions

# compare normalized median ratios of two GAL4 lines
selGAL4 <- c("OR83B","NP1227")
selAB <- c("Syd1")
wilcox.test(glist[[selGAL4[1]]][[selAB]]$median,glist[[selGAL4[2]]][[selAB]]$median)

# generate boxplot only using the ratio medians
# note that the number of measurements can differ between GAL4 lines
pData <- data.frame(cbind(selGAL4[1],glist[[selGAL4[1]]][[selAB]]$median))
pData <- rbind(pData,data.frame(cbind(selGAL4[2],glist[[selGAL4[2]]][[selAB]]$median)))
colnames(pData) <- c("GAL4","median")
pData$median <- as.numeric(as.character(pData$median))

ggplot(pData, aes(x=GAL4,y=median)) +geom_boxplot(aes(fill=GAL4),size=I(1.5)) +
  scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio ",selAB,"/BrpCT")) +
  theme_bw() +theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"))


#########################################################
# ADDENDUM: Why the KS test cannot be used in this case #
#########################################################
# manually load data sets
gal4s <- c("OR83B","NP1227")
ab <- c("RBP")

data <- loadComp(dDir,gal4s,ab,ratios) # read GAL4/GFP files only (no reference) and bin the data
data <- loadCompBoth(dDir,gal4s,ab,ratios) # read GFP+reference, subtract reference from GAL4/GFP and bin data
head(data)

# KS test cannot be used for comparing these distributions because:
# 1. Only the shape of the curve is relevant for KS.
# 2. Absolute values are being considered for KS.
# 3. Position of the curve on the x-axis is irrelevant for KS.
ks.test(data$OR83B,data$NP1227, alternative="two.sided")

# plot histograms
display <- 16
ggplot(data, aes(x=ori_ratio,y=OR83B)) +geom_bar(stat="identity",fill="forestgreen") +scale_x_continuous(trans='log2', breaks=c(1/16,1/8,1/4,1/2,1,2,4,8,16), labels=c("1/16","1/8","1/4","1/2","1","2","4","8","16")) +scale_y_continuous(breaks=c(seq(-1,1,0.005))) +coord_cartesian(xlim=c(1/display,display)) +xlab(paste("Ratio",gal4s[1],ab)) +ylab("Difference in Normalized Frequency") +theme_bw() +theme(axis.text.y=element_blank(), panel.grid.major=element_line(colour="grey60"))
ggplot(data, aes(x=ori_ratio,y=NP1227)) +geom_bar(stat="identity",fill="forestgreen") +scale_x_continuous(trans='log2', breaks=c(1/16,1/8,1/4,1/2,1,2,4,8,16), labels=c("1/16","1/8","1/4","1/2","1","2","4","8","16")) +scale_y_continuous(breaks=c(seq(-1,1,0.005))) +coord_cartesian(xlim=c(1/display,display)) +xlab(paste("Ratio",gal4s[2],ab)) +ylab("Difference in Normalized Frequency") +theme_bw() +theme(axis.text.y=element_blank(), panel.grid.major=element_line(colour="grey60"))

# plot ECDFs
library(reshape2)
test <- melt(data,measure=c("OR83B","NP1227"))
ggplot(data=test,aes(x=value,color=variable)) +stat_ecdf(size=1.5) + 
  scale_x_continuous() +
  scale_y_continuous(breaks=seq(0,1,0.1)) +
  ylab("Cumulative probability") +xlab("Frequency") +theme_bw() +
  theme(legend.title=element_blank(),legend.text.align=0, panel.grid.major=element_line(color="grey60"), axis.text.x=element_text(size=12), axis.text.y=element_text(size=12), axis.title.x=element_text(size=16), axis.title.y=element_text(size=16)) +
  scale_color_brewer(palette="Dark2")
