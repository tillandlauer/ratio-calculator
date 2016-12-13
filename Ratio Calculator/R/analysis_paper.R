####################################################
# FUNCTIONS FOR PLOTTING AND ANALYZING RATIO DATA  #
# v1.0, 16-12-12, Till Andlauer, till@andlauer.net #
####################################################
library(ggplot2)
setwd("/Users/till/Documents/ownCloud/GWDG/Misc/Ratios/analysis")
source("ratioFunctions.R")
ratios <- read.table("ratios.txt",h=T,sep="\t") # ratios conversion table
dDir <- "data/"

#################
# Violin plots #
#################
gal4s <- c("OR83B","NP1227") # names
ab <- c("Syd1")

# load data
for (j in seq(1,length(gal4s)))
  {
  files <- listFiles(dDir,gal4s[j]) # count the number of measurements per GAL4 line
  numFiles <- files[[ab]] # number of files
  all <- readFiles(dDir,gal4s[j],ab,numFiles)
  aGFP <- readFiles(dDir,gal4s[j],ab,numFiles,gfp=T)

  tempA <- all[,c(ncol(all)-3,ncol(all))] # mean
  colnames(tempA)[1] <- "norm"
  merged <- merge(ratios,tempA,by.x="finalrank",by.y="X")
  merged <- merged[-which(duplicated(merged$finalrank)),]
  data <- merged[with(merged,order(merged$ratio)),]
  data["cat"] <- rep(paste0(gal4s[j],"_AL"),length(data$X))
    
  tempG <- aGFP[,c(ncol(aGFP)-3,ncol(aGFP))] # mean
  colnames(tempG)[1] <- "norm"
  merged <- merge(ratios,tempG,by.x="finalrank",by.y="X")
  merged <- merged[-which(duplicated(merged$finalrank)),]
  data2 <- merged[with(merged,order(merged$ratio)),]
  data2["cat"] <- rep(paste0(gal4s[j],"_GFP"),length(data2$X))
    
  temp <- rbind(data,data2)
  if(j==1) pData <- temp else pData <- rbind(pData,temp)
  }
pData$cat <- factor(pData$cat,levels=unique(pData$cat),labels=unique(pData$cat)) # correct order
pData["inv_ratio"] <- 1/pData$ratio # invert for Brp/Syd

# violin plots
display <- 4
library(RColorBrewer)
pal2 <- brewer.pal(4, "Paired")[c(1,3,2,4)]
ggplot(pData, aes(x=cat, y=inv_ratio, weight=norm)) +geom_violin(aes(fill=cat),adjust=0.2,width=0.8) +
  geom_boxplot(fill="gray90",width=0.3,outlier.shape=NA) +
  scale_y_continuous(trans='log2', breaks=c(1/4,1/3,1/2,2/3,1,1.5,2,3,4), labels=c("1/4","1/3","1/2","2/3","1","1.5","2","3","4")) +
  coord_cartesian(ylim=c(1/display,display)) +scale_fill_manual(values=pal2) +
  ylab(paste0("Ratio BrpCT/",ab)) +theme_bw() +
  theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"))
ggsave(paste0("paper/violin.pdf"))

#############################################################
# Plot quantiles and medians of GAL4/GFP lines per antibody #
#############################################################
# calculate quantile ratios, normalize GAL4/GFP ratios by reference neuropil 
# use weighted quantiles: calculate quantile ratios counting the normalized frequencies per file
# normalize GAL4/GFP quantiles by reference neuropil quantiles
# note that the normalized min/max quantiles cannot be interpreted

glist <- readRDS("gal4_lines.RDS")
glist <- glist[gal4s]

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
pData$value <- 1/pData$value # invert for Brp/Syd
head(pData)  

# plot boxplots of medians
display <- c(0.8,1.45)
ggplot(pData, aes(x=variable,y=value)) +geom_boxplot(aes(fill=variable),size=I(1)) +
  scale_fill_brewer(palette="Paired") +ylab(paste0("Relative enrichment of median ratio BrpCT/",ab)) +
  coord_cartesian(ylim=display) +theme_bw() +scale_y_continuous(breaks=seq(0,2,0.05)) +
  theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5), panel.grid.major.x=element_blank(), panel.grid.minor.y=element_blank())
ggsave(paste0("paper/medians.pdf"))  

#################################################
# Compare all GAL4 lines per antibody: MWU test #
#################################################
abList <- ab
for (j in seq(1,length(glist)-1))
  {
  if(any(names(glist[[j]])==abList[i]))
    {
    for (k in seq(j+1,length(glist)))
      {
      if(any(names(glist[[k]])==abList[i]))
        {
        temp <- format(wilcox.test(glist[[j]][[abList[i]]]$median,glist[[k]][[abList[i]]]$median)$p.value,dig=3,sci=T)
        allP <- data.frame(cbind(names(glist)[j],names(glist)[k],temp),stringsAsFactors=F)
        }
      }
    }
  }
allP["significant"] <- ""
allP[which(as.numeric(allP$temp)<(0.05/c)),]$significant <- "*"  
colnames(allP)[1:3] <- c("GAL4_1","GAL4_2","p-value")
print(allP)
write.table(allP,paste0("paper/mwu_test.txt"),c=T,r=F,qu=F)
