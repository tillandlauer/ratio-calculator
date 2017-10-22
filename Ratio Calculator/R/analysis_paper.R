####################################################
# ANALYSIS OF RATIO DATA (PAPER-SPECIFIC VERSION)  #
# v1.1, 17-06-11, Till Andlauer, till@andlauer.net #
####################################################
# setwd("/Users/till/Documents/ownCloud/GWDG/Misc/Ratios/Ratio\ Analysis")

library(ggplot2)
source("ratioFunctions.R")

ratios <- read.table("ratios.txt",h=T,sep="\t") # ratios conversion table
dir.create(paste0("paper"),showWarnings=F) # create plot directory
dir.create(paste0("paper/plots"),showWarnings=F) # create plot directory

#################
# Antennal lobe #
#################
glist <- readRDS("gal4_lines.RDS") # load previously generated ratio data

gal4s <- c("OR83B","NP1227")
ab <- c("Syd1")
glist <- glist[gal4s]

boxplot_ab("paper/", ab, glist, minBreaks=12, invert=T)

result <- mwu_ABratios(ab, glist)
write.csv(result,paste0("paper/mwu_test_AL_syd1.csv"),col.names=T,row.names=F)

#########
# Calyx #
#########
glist <- readRDS("gal4_lines_calyx.RDS") # load previously generated ratio data

gal4s <- c("Mz19","17D")
ab <- c("Syd1")
glist <- glist[gal4s]

boxplot_ab("paper/", ab, glist, minBreaks=12, invert=T)

result <- mwu_ABratios(ab, glist)
write.csv(result,paste0("paper/mwu_test_calyx_syd1.csv"),col.names=T,row.names=F)
