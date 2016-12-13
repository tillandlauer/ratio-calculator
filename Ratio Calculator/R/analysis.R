####################################################
# FUNCTIONS FOR PLOTTING AND ANALYZING RATIO DATA  #
# v1.0, 16-12-12, Till Andlauer, till@andlauer.net #
####################################################
library(ggplot2)
setwd("/Users/till/Documents/ownCloud/GWDG/Misc/Ratios/analysis")
source("ratioFunctions.R")
ratios <- read.table("ratios.txt",h=T,sep="\t") # ratios conversion table
dDir <- "data/"

#######################
# Plot all GAL4 lines #
#######################
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

#####################################
# Plot single GAL4 lines (as above) #
#####################################
gal4 <- "OR65A_incomplete"
files <- listFiles(dDir,gal4)
for (i in seq(1,length(files)))
  {
  ab <- colnames(files)[i]
  numFiles <- files[[i]]
  plotBoth(dDir,gal4,ab,numFiles,ratios)
  plotDiff(dDir,gal4,ab,numFiles,ratios)
  }

##########################################
# Compare differences between situations #
##########################################
# calculate quantile ratios, normalize GAL4/GFP ratios by reference neuropil 
# use weighted quantiles: calculate quantile ratios counting the normalized frequencies per file
# normalize GAL4/GFP quantiles by reference neuropil quantiles
# note that the normalized min/max quantiles cannot be interpreted

gal4s <- c("OR83B","OR46A","OR47B","OR67D_DA1","OR67D_VA6","NP1227","Krasa","Mz19")
# glist <- genQuant(dDir,gal4s,ratios)
# saveRDS(glist,"gal4_lines.RDS")
glist <- readRDS("gal4_lines.RDS")
glist$OR83B

#########################################
# Example: two GAL4 lines, one antibody #
#########################################
# compare normalized median ratios of two GAL4 lines
selGAL4 <- c("OR83B","NP1227")
selAB <- c("Syd1")
wilcox.test(glist[[selGAL4[1]]][[selAB]]$median,glist[[selGAL4[2]]][[selAB]]$median)

# plot quantiles
pData <- data.frame(rbind(colMeans(glist[[selGAL4[1]]][[selAB]]),colMeans(glist[[selGAL4[2]]][[selAB]])))
pData["GAL4"] <- factor(selGAL4,levels=selGAL4,labels=selGAL4)
ggplot(pData, aes(x=GAL4,ymin=min,lower=q25,middle=median,upper=q75,ymax=max)) +
  geom_boxplot(stat="identity",aes(fill=GAL4),size=I(1.5)) +
  scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of ratio ",ab,"/BrpCT")) +
  theme_bw() +theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"))

# plot boxplot of medians
library(reshape2)
pData <- data.frame(cbind(glist[[selGAL4[1]]][[selAB]]$median,glist[[selGAL4[2]]][[selAB]]$median))
colnames(pData) <- selGAL4
pData <- melt(pData)
ggplot(pData, aes(x=variable,y=value)) +geom_boxplot(aes(fill=variable),size=I(1.5)) +
  scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio ",ab,"/BrpCT")) +
  theme_bw() +theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"))

#########################################################################
# Compare all normalized median antibody ratios per GAL4 line: MWU test #
#########################################################################
sink("mwu_tests_antibodies.txt")
for (i in seq_along(glist))
  {
  writeLines(paste0(names(glist)[i],":"))
  for (j in seq_along(glist[[i]]))
    {
    temp <- as.data.frame(glist[[i]][[j]])
    temp["Antibody"] <- names(glist[[i]])[j]
    if(j==1) pData <- temp else pData <- rbind(pData,temp)
    }
  pData$Antibody <- factor(pData$Antibody)
  writeLines(paste0("Kruskal-Wallis-Test:\t",format(kruskal.test(pData$median,pData$Antibody)$p.value,dig=3,sci=T)))
  for (j in seq(1,length(glist[[i]])-1))
    {
    for (k in seq(j+1,length(glist[[i]])))
      {
      writeLines(paste0(names(glist[[i]])[j]," vs. ",names(glist[[i]])[k],":\t\t",format(wilcox.test(glist[[i]][[j]]$median,glist[[i]][[k]]$median)$p.value,dig=3,sci=T)))
      }
    }
  writeLines("")
  }
writeLines(paste0("Bonferroni GAL4:\t",format(0.05/length(glist),dig=3,sci=T)))
writeLines(paste0("Bonferroni 3 Tests:\t",format(0.05/3,dig=3,sci=T)))
sink(NULL)

############################################################################
# Compare all normalized median antibody ratios per GAL4 line: Dunn's test #
############################################################################
library(dunn.test)
for (i in seq_along(glist))
  {
  writeLines(paste0(names(glist)[i],":"))
  for (j in seq_along(glist[[i]]))
    {
    temp <- as.data.frame(glist[[i]][[j]])
    temp["Antibody"] <- names(glist[[i]])[j]
    if(j==1) pData <- temp else pData <- rbind(pData,temp)
    }
  dunn.test(pData$median,pData$Antibody)
  }

#####################################################################
# Assess whether the normalized median rations are different from 1 #
#####################################################################
sink("mwu_tests_antibodies_different.txt")
for (i in seq_along(glist))
  {
  writeLines(paste0(names(glist)[i],":"))
  for (j in seq(1,length(glist[[i]])))
    {
    writeLines(paste0(names(glist[[i]])[j]," (MWU):\t\t",format(wilcox.test(glist[[i]][[j]]$median,rep(1,nrow(glist[[i]][[j]])))$p.value,dig=3,sci=T)))
    }
  for (j in seq(1,length(glist[[i]])))
    {
    writeLines(paste0(names(glist[[i]])[j]," (t):\t\t",format(t.test(glist[[i]][[j]]$median-1)$p.value,dig=3,sci=T)))
    }
  writeLines("")
  }
writeLines(paste0("Bonferroni GAL4:\t",format(0.05/length(glist),dig=3,sci=T)))
writeLines(paste0("Bonferroni 3 Tests:\t",format(0.05/3,dig=3,sci=T)))
sink(NULL)

##############################################################
# Plot quantiles and medians of antibodies per GAL4/GFP line #
##############################################################
for (i in seq_along(gal4s))
  {
  gal4 <- gal4s[i]
  
  # calculate means of quantiles
  for (j in seq_along(glist[[gal4]]))
    {
    temp <- data.frame(t(colMeans(glist[[gal4]][[j]])))
    temp["Antibody"] <- names(glist[[gal4]])[j]
    if(j==1) pData <- temp else pData <- rbind(pData,temp)
    }

  # plot quantiles
  display <- c(min(pData$q25)-min(pData$q25)/10,max(pData$q75)+max(pData$q75)/10) # y-axis limits of plots
  ggplot(pData, aes(x=Antibody,ymin=min,lower=q25,middle=median,upper=q75,ymax=max)) +
    geom_boxplot(stat="identity",aes(fill=Antibody),size=I(1)) +
    scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of ratio Antibody/BrpCT")) +
    coord_cartesian(ylim=display) +theme_bw() +ggtitle(gal4) +
    theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5))
  ggsave(paste0(dDir,"plots/",gal4,"_quantiles.pdf"))  

  # summarize medians
  for (j in seq_along(glist[[gal4]]))
    {
    temp <- data.frame(glist[[gal4]][[j]]$median)
    colnames(temp) <- "value"
    temp["variable"] <- names(glist[[gal4]])[j]
    if(j==1) pData <- temp else pData <- rbind(pData,temp)
    }
  pData$variable <- factor(pData$variable,levels=unique(pData$variable),labels=unique(pData$variable)) # correct order
  
  # plot boxplots of medians
  ggplot(pData, aes(x=variable,y=value)) +geom_boxplot(aes(fill=variable),size=I(1)) +
    scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio Antibody/BrpCT")) +
    coord_cartesian() +theme_bw() +ggtitle(gal4) +
    theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5))
  ggsave(paste0(dDir,"plots/",gal4,"_medians.pdf"))  
  }

#################################################
# Compare all GAL4 lines per antibody: MWU test #
#################################################
abList <- c("BrpNT","RBP","Syd1")

sink("mwu_tests_gal4.txt")
for (i in seq_along(abList))
  {
  c <- 0
  if(exists("allP")) rm(allP)
  writeLines(paste0(abList[i],":"))
  for (j in seq_along(glist))
    {
    if(any(names(glist[[j]])==abList[i]))
      {
      temp <- data.frame(glist[[j]][[abList[i]]])
      temp["GAL4"] <- names(glist)[[j]]
      if(j==1) pData <- temp else pData <- rbind(pData,temp)
      }
    }
  pData$GAL4 <- factor(pData$GAL4)
  writeLines(paste0("Kruskal-Wallis-Test:\t",format(kruskal.test(pData$median,pData$GAL4)$p.value,dig=3,sci=T),"\n"))
  for (j in seq(1,length(glist)-1))
    {
    if(any(names(glist[[j]])==abList[i]))
      {
      for (k in seq(j+1,length(glist)))
        {
        if(any(names(glist[[k]])==abList[i]))
          {
          temp <- format(wilcox.test(glist[[j]][[abList[i]]]$median,glist[[k]][[abList[i]]]$median)$p.value,dig=3,sci=T)
          c <- c+1
          temp <- data.frame(cbind(names(glist)[j],names(glist)[k],temp),stringsAsFactors=F)
          if(!exists("allP")) allP <- temp else allP <- rbind(allP,temp) 
          }
        }
      }
    }
  allP["significant"] <- ""
  allP[which(as.numeric(allP$temp)<(0.05/c)),]$significant <- "*"  
  colnames(allP)[1:3] <- c("GAL4_1","GAL4_2","p-value")
  print(allP)
  writeLines(paste0("\nBonferroni correction:\t",format(0.05/c,dig=3,sci=T),"\n"))
  }
sink(NULL)

#############################################################
# Plot quantiles and medians of GAL4/GFP lines per antibody #
#############################################################
for (i in seq_along(abList))
  {
  ab <- abList[i]

  # determine which antibodies are present for which GAL4 line and summarize only these
  # calculate means of quantiles
  gF <- data.frame(cbind(gal4s,rep(F,length(gal4s))),stringsAsFactors=F)
  colnames(gF) <- c("GAL4","FOUND")
  for (j in seq_along(glist))
    {
    if(any(names(glist[[j]])==ab))
      {
      temp <- data.frame(t(colMeans(glist[[j]][[ab]])))
      temp["GAL4"] <- names(glist)[[j]]
      if(j==1) pData <- temp else pData <- rbind(pData,temp)
      gF[which(gF$GAL4==names(glist)[[j]]),]$FOUND <- T
      }
    }
  temp <- gF[which(gF$FOUND=="TRUE"),]$GAL4  
  pData["GAL4"] <- factor(temp,levels=temp,labels=temp)

  # plot quantiles
  display <- c(min(pData$q25)-min(pData$q25)/10,max(pData$q75)+max(pData$q75)/10) # y-axis limits of plots
  ggplot(pData, aes(x=GAL4,ymin=min,lower=q25,middle=median,upper=q75,ymax=max)) +
    geom_boxplot(stat="identity",aes(fill=GAL4),size=I(1)) +
    scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of ratio ",ab,"/BrpCT")) +
    coord_cartesian(ylim=display) +theme_bw() +ggtitle(ab) +
    theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5))
  ggsave(paste0(dDir,"plots/",ab,"_quantiles.pdf"))  

  # summarize medians
  for (j in seq_along(glist))
    {
    if(any(names(glist[[j]])==ab))
      {
      temp <- data.frame(glist[[j]][[ab]]$median)
      colnames(temp) <- "value"
      temp["variable"] <- names(glist)[[j]]
      if(j==1) pData <- temp else pData <- rbind(pData,temp)
      }
    }
  pData$variable <- factor(pData$variable,levels=unique(pData$variable),labels=unique(pData$variable)) # correct order

  # plot boxplots of medians
  ggplot(pData, aes(x=variable,y=value)) +geom_boxplot(aes(fill=variable),size=I(1)) +
    scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio ",ab,"/BrpCT")) +
    coord_cartesian() +theme_bw() +ggtitle(ab) +
    theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5))
  ggsave(paste0(dDir,"plots/",ab,"_medians.pdf"))  
  }

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
