####################################################
# FUNCTIONS FOR PLOTTING AND ANALYZING RATIO DATA  #
# v1.1, 17-06-11, Till Andlauer, till@andlauer.net #
####################################################

require(ggplot2)

# count the number of samples in a directory
listFiles <- function(dDir,gal4,abs=c("BrpNT","RBP","Syd1"))
  {
  for (i in seq(1,length(abs)))
    {
    if(i==1) {dirState <- dir.exists(paste0(dDir,gal4,"/",abs[i]))
      } else {dirState <- c(dirState,dir.exists(paste0(dDir,gal4,"/",abs[i])))}
    }
  abs <- abs[which(dirState)]
  for (i in seq(1,length(abs)))
    {
    if(i==1) {nFiles <- length(list.files(paste0(dDir,gal4,"/",abs[i]),pattern="^[0-9]",include.dirs=F))
      } else {nFiles <- c(nFiles,length(list.files(paste0(dDir,gal4,"/",abs[i]),pattern="^[0-9]",include.dirs=F)))}
    }
  nFiles <- data.frame(t(nFiles))
  colnames(nFiles) <- abs
  
  return(nFiles)
  }

# read all samples and combine them into a data frame
readFiles <- function(dDir="",gal4,ab,numFiles,gfp=F) # GFP==T for reading a subset of GAL4/GFP data
  {
  extra <- ""
  if (gfp) {extra <- "/GFP"}
  for (i in 1:numFiles)
    {
    if (i<10)
      {
      temp <- read.table(paste0(dDir,gal4,"/",ab,extra,"/0",i," Original Data.xls"),h=T,sep="\t")
    } else
      {
      temp <- read.table(paste0(dDir,gal4,"/",ab,extra,"/",i," Original Data.xls"),h=T,sep="\t")
      }
    temp["norm"] <- temp$frequency/sum(temp$frequency) # normalize individual counts by total number of counts (to normalize for neuropil size)
    if (i==1)
      {
      all <- data.frame(temp$norm)
    } else
      {
      all <- cbind(all,data.frame(temp$norm))
      }
    colnames(all)[i] <- paste0("file",i)
    }

  allOut <- all
  allOut["mean"] <- apply(all,1,mean)
  allOut["sd"] <- apply(all,1,sd)
  allOut["median"] <- apply(all,1,median)
  allOut["X"] <- rownames(all)
  return(allOut)  
  }

# read both GFP and reference neuropil data and subtract the reference neuropil from GAL4/GFP
readBoth <- function(dDir="",gal4,ab,numFiles)
  {
  all <- readFiles(dDir,gal4,ab,numFiles)
  aGFP <- readFiles(dDir,gal4,ab,numFiles,gfp=T)
  
  temp <- all[,1:numFiles]
  for (i in 1:numFiles)
    {
    temp[,i] <- aGFP[,i]-all[,i]
    }
  
  allOut <- temp
  allOut["mean"] <- apply(temp,1,mean)
  allOut["sd"] <- apply(temp,1,sd)
  allOut["median"] <- apply(temp,1,median)
  allOut["X"] <- rownames(temp)
  return(allOut)  
  }

# bin the data as preparation for plotting histograms
generateHisto <- function(data, ratios, bins)
  {
  merged <- merge(ratios,data,by.x="finalrank",by.y="X")
  merged <- merged[-which(duplicated(merged$finalrank)),]

  stepList <- seq(log2(1/256),log2(256),2*log2(256)/bins)
  data <- merged[with(merged,order(merged$ratio)),]
  data["bin"] <- cut(log2(data$ratio),stepList,include.lowest=T,labels=seq(1,bins,1))
  data["cut"] <- cut(log2(data$ratio),stepList,include.lowest=T)
  data <- data[,c(1,6:ncol(data))]

  for (i in 1:bins)
    {
    temp <- data[which(data$bin %in% i),]  
    if(i==1) {histo <- sum(temp$norm)} else {histo <- append(histo,sum(temp$norm))}
    }
  histo <- as.data.frame(histo)
  colnames(histo) <- "freq"
  histo["ratio"] <- stepList[1:bins]
  histo["ori_ratio"] <- 2^stepList[1:bins]
  
  return(histo)
  }

# plot both histograms and violin plots
plotBoth <- function(dDir,gal4,ab,numFiles,ratios,bins=256,display=16)
  {
  all <- readFiles(dDir,gal4,ab,numFiles)
  aGFP <- readFiles(dDir,gal4,ab,numFiles,gfp=T)
  dir.create(paste0(dDir,"/plots"),showWarnings=F) # create plot directory
  
  # histogram of the reference neuropil
  tempA <- all[,c(ncol(all)-3,ncol(all))] # mean
  colnames(tempA)[1] <- "norm"
  histo <- generateHisto(tempA, ratios, bins)
  ggplot(histo, aes(x=ori_ratio,y=freq)) +geom_bar(stat="identity",fill="dodgerblue4") +
  	scale_x_continuous(trans='log2', breaks=c(1/16,1/8,1/4,1/2,1,2,4,8,16), labels=c("1/16","1/8","1/4","1/2","1","2","4","8","16")) +
  	scale_y_continuous(breaks=c(seq(0,1,0.005))) +coord_cartesian(xlim=c(1/display,display),ylim=c(0,0.05)) +
  	xlab(paste("Ratio",gal4,ab)) +ylab("Normalized Frequency") +theme_bw() +
  	theme(axis.text.y=element_blank(),panel.grid.major=element_line(colour="grey60"))
  ggsave(paste0(dDir,"plots/",gal4,"_",ab,"_complete.pdf"))
  
  # histogram of GAL4/GFP region
  tempG <- aGFP[,c(ncol(aGFP)-3,ncol(aGFP))] # mean
  colnames(tempG)[1] <- "norm"
  histo <- generateHisto(tempG, ratios, bins)
  ggplot(histo, aes(x=ori_ratio,y=freq)) +geom_bar(stat="identity",fill="forestgreen") +
  	scale_x_continuous(trans='log2', breaks=c(1/16,1/8,1/4,1/2,1,2,4,8,16), labels=c("1/16","1/8","1/4","1/2","1","2","4","8","16")) +
  	scale_y_continuous(breaks=c(seq(0,1,0.005))) +coord_cartesian(xlim=c(1/display,display),ylim=c(0,0.05)) +
  	xlab(paste("Ratio",gal4,ab,"GFP")) +ylab("Normalized Frequency") +theme_bw() +
  	theme(axis.text.y=element_blank(), panel.grid.major=element_line(colour="grey60"))
  ggsave(paste0(dDir,"plots/",gal4,"_",ab,"_GFP.pdf"))
  
  # violin plots
  merged <- merge(ratios,tempA,by.x="finalrank",by.y="X")
  merged <- merged[-which(duplicated(merged$finalrank)),]
  data <- merged[with(merged,order(merged$ratio)),]
  data["cat"] <- rep("AL",length(data$X))
  
  merged <- merge(ratios,tempG,by.x="finalrank",by.y="X")
  merged <- merged[-which(duplicated(merged$finalrank)),]
  data2 <- merged[with(merged,order(merged$ratio)),]
  data2["cat"] <- rep("GFP",length(data2$X))
  
  data <- rbind(data,data2)
  
  ggplot(data, aes(x=cat, y=ratio, weight=norm)) +geom_violin(aes(fill=cat),adjust=0.2,width=0.8) +
  	geom_boxplot(fill="gray90",width=0.3,outlier.shape=NA) +
  	scale_y_continuous(trans='log2', breaks=c(1/16,1/8,1/4,1/2,1,2,4,8,16), labels=c("1/16","1/8","1/4","1/2","1","2","4","8","16")) +
  	coord_cartesian(ylim=c(1/display,display)) +scale_fill_manual(values=c("dodgerblue4","forestgreen")) +
  	ylab(paste0("Ratio ",ab,"/BrpCT")) +theme_bw() +
  	theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"))
  ggsave(paste0(dDir,"plots/",gal4,"_",ab,"_violin.pdf"))
  }

# plot the difference between GFP and reference neuropil as a histogram
# subtract the reference neuropil from the GAL4/GFP histogram
plotDiff <- function(dDir,gal4,ab,numFiles,ratios,bins=256,display=16)
  {
  allD <- readBoth(dDir,gal4,ab,numFiles) # reads files and directly subtracts the reference from GAL4/GFP
  tempD <- allD[,c(ncol(allD)-3,ncol(allD))] # mean
  colnames(tempD)[1] <- "norm"
  histo <- generateHisto(tempD, ratios, bins)
  dir.create(paste0(dDir,"/plots"),showWarnings=F) # create plot directory
  
  ggplot(histo, aes(x=ori_ratio,y=freq)) +geom_bar(stat="identity",fill="forestgreen") +
  	scale_x_continuous(trans='log2', breaks=c(1/16,1/8,1/4,1/2,1,2,4,8,16), labels=c("1/16","1/8","1/4","1/2","1","2","4","8","16")) +
  	scale_y_continuous(breaks=c(seq(-1,1,0.005))) +coord_cartesian(xlim=c(1/display,display),ylim=c(-0.03,0.03)) +
  	xlab(paste("Ratio",gal4,ab)) +ylab("Difference in Normalized Frequency") +theme_bw() +
  	theme(axis.text.y=element_blank(), panel.grid.major=element_line(colour="grey60"))
  ggsave(paste0(dDir,"plots/",gal4,"_",ab,"_difference.pdf"))
  }

# read GAL4/GFP files only (no reference) and bin the data (for manual plotting)
# note that this function has not been used in the final analysis and is not actively maintained
loadComp <- function(dDir,gal4s,ab,ratios,bins=64)
  {
  for (j in seq(1,length(gal4s)))
    {
    print(paste0("Loading ",gal4s[j],"..."))
    numFiles <- as.numeric(listFiles(dDir,gal4s[j],ab))
    
    allD <- readFiles(dDir,gal4s[j],ab,numFiles,gfp=T)
    tempD <- allD[,c(ncol(allD)-3,ncol(allD))] # mean
    colnames(tempD)[1] <- "norm"
    if(j==1) { histo <- generateHisto(tempD, ratios, bins)
    } else
      {
      temp <- generateHisto(tempD, ratios, bins)
      histo <- list(histo,temp)
      }
    }
  names(histo) <- gal4s
  
  data <- histo[[1]][,c(2,3,1)]
  colnames(data)[3] <- gal4s[1]
  for (i in seq(2,length(gal4s)))
    {
    data[paste0(gal4s[i])] <- histo[[i]][,1]
    }
  return(data)
  }

# read both GFP and reference neuropil data and subtract the reference neuropil from GAL4/GFP, bin the data (for manual plotting)
# note that this function has not been used in the final analysis and is not actively maintained
loadCompBoth <- function(dDir,gal4s,ab,ratios,bins=64)
  {
  for (j in seq(1,length(gal4s)))
    {
    print(paste0("Loading ",gal4s[j],"..."))
    numFiles <- as.numeric(listFiles(dDir,gal4s[j],ab))

    allD <- readBoth(dDir,gal4s[j],ab,numFiles)
    tempD <- allD[,c(ncol(allD)-3,ncol(allD))] # mean
    colnames(tempD)[1] <- "norm"
    if(j==1) { histo <- generateHisto(tempD, ratios, bins)
    } else
      {
      temp <- generateHisto(tempD, ratios, bins)
      histo <- list(histo,temp)
      }
    }
  names(histo) <- gal4s
  
  data <- histo[[1]][,c(2,3,1)]
  colnames(data)[3] <- gal4s[1]
  for (i in seq(2,length(gal4s)))
    {
    data[paste0(gal4s[i])] <- histo[[i]][,1]
    }
  return(data)
  }

# calculate normalized quantile ratios for several GAL4 lines, wrapper function for calcQuant()
genQuant <- function(dDir,gal4s,ratios)
 	{
	for (j in seq_along(gal4s))
		{
		print(paste0("Loading ",gal4s[j],"..."))
		files <- listFiles(dDir,gal4s[j])
		for (i in seq_along(files))
			{
			ab <- colnames(files)[i]
			numFiles <- files[[i]]
			temp <- calcQuant(dDir,gal4s[j],ab,numFiles,ratios)
			if(i==1) gtemp <- list(temp) else gtemp <- c(gtemp,list(temp))
			}
		names(gtemp) <- colnames(files)
		if(j==1) glist <- list(gtemp) else glist <- c(glist,list(gtemp))
		}

	names(glist) <- gal4s
	return(glist)
	}

# calculate quantile ratios for one GAL4 line and one antibody, normalize GAL4/GFP ratios by reference neuropil 
calcQuant <- function(dDir,gal4,ab,numFiles,ratios)
  {
  all <- readFiles(dDir,gal4,ab,numFiles)
  aGFP <- readFiles(dDir,gal4,ab,numFiles,gfp=T)
  
  all <- merge(ratios,all,by.x="finalrank",by.y="X")
  all <- all[-which(duplicated(all$finalrank)),]

  aGFP <- merge(ratios,aGFP,by.x="finalrank",by.y="X")
  aGFP <- aGFP[-which(duplicated(aGFP$finalrank)),]

  for(i in seq(1,numFiles)) # calculate for each file separately
    {
    # weighted quantiles: calculate quantile ratios counting the normalized frequencies per file
    # normalize GAL4/GFP quantiles by reference neuropil
    # note that the results are GFP ratios relative to the reference neuropil ratios
    # each numeric, relative/normalized value needs to be interpreted on its own
    # because the values are relative, the 25% quantile can have a higher value than the 75% quantile
    # the normalized min/max quantiles cannot be interpreted and are therefore omitted

    # qmin <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0)
    q1 <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0.25)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0.25)
    qmed <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0.5)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0.5)
    q3 <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0.75)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0.75)
    # qmax <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=1)/wtd_quant(all$ratio,weight=all[,(6+i)],q=1)

    # temp <- data.frame(t(c(qmin,q1,qmed,q3,qmax)))
    # colnames(temp) <- c("min","q25","median","q75","max")
    temp <- data.frame(t(c(q1,qmed,q3)))
    colnames(temp) <- c("q25","median","q75")
    if(i==1) dV <- temp else dV <- rbind(dV,temp)
    }

  return(dV)
  }

# The following method is described in the book Relative Distribution Methods in the Social Sciences by Handcock and Morris, copyright 1999 Springer-Verlag, Inc., ISBN 0387987789, and is transmitted by permission of Springer-Verlag, Inc. 
# Copyright (c) 1999 Mark S. Handcock 
wtd_quant <- function (x, q=0.5, na.rm=FALSE, weight=FALSE) # function reformatted but without relevant modifications adopted from package reldist 1.6.4 because the function has been removed in reldist 1.6.6
  {
  if (mode(x) != "numeric") stop("need numeric data")
  
  x <- as.vector(x)
  wnas <- is.na(x)
  
  if (sum(wnas) > 0) 
    {
    if (na.rm) x <- x[!wnas]
    if (!missing(weight)) 
      {
      weight <- weight[!wnas]
      } else return(NA)
    }
  
  n <- length(x)
  half <- (n + 1)/2
  
  if (n%%2 == 1) 
    {
    if (!missing(weight)) 
      {
      weight <- weight/sum(weight)
      sx <- sort.list(x)
      sweight <- cumsum(weight[sx])
      min(x[sx][sweight >= q])
      } else x[order(x)[half]] 
  } else 
    {
    if (!missing(weight)) 
      {
      weight <- weight/sum(weight)
      sx <- sort.list(x)
      sweight <- cumsum(weight[sx])
      min(x[sx][sweight >= q])
    } else 
      {
      half <- floor(half) + 0:1
      sum(x[order(x)[half]])/2
      }
    }
  }

# Compare all normalized median antibody ratios per GAL4 line: Mann-Whitney U test
# This analysis tests whether antibody ratios differ within a GAL4 line
# Results are shown on screen
mwu_gal4ratios <- function(glist)
  {
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
  }

# Compare all normalized median antibody ratios per GAL4 line: Dunn's test
# This analysis tests whether antibody ratios differ within a GAL4 line
# Results are shown on screen
dunn_gal4ratios <- function(glist)
  {
  require(dunn.test)

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
  }

# Test whether the normalized median rations are different from 1
# Uses both Mann-Whitney U test and Student's t test
# Results are shown on screen
median_diff <- function(glist)
  {
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
  }

# Compare all normalized median ratios per antibody between GAL4 lines: Mann-Whitney U test
# This analysis tests whether antibody ratios differ between GAL4 lines
# Results are shown on screen
mwu_ABratios <- function(abList, glist)
  {
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

    # calculate significance levels
    allP["significant"] <- F
    bonf <- 0.05/c
    allP$temp <- as.numeric(as.character(allP$temp))
    if(any(allP$temp<bonf)) allP[which(allP$temp<bonf),]$significant <- T  
    allP["stars"] <- getStars(allP$temp*c)
    colnames(allP)[1:3] <- c("GAL4_1","GAL4_2","p-value")

    print(allP)
    writeLines(paste0("\nBonferroni correction:\t",format(bonf,dig=3,sci=T),"\n"))
    }

  return(allP)
  }

# calculate stars for significance levels
getStars <- function(data) 
  {
  for(i in 1:length(data))
    {
    if(data[i]<0.001) {temp <- "***"
    } else if(data[i]<0.01) {temp <- "**"
    } else if(data[i]<0.05) {temp <- "*"
    } else {temp <- "ns"}
    
    if(i==1) result <- temp else result <- c(result,temp)
    }
  return(result)
  }

# Plot median ratios of antibodies per GAL4/GFP line as boxplots
boxplot_gal4 <- function(dDir, gal4s, glist, minBreaks=5) # dDir is the base directory where plots are saved
  {
  for (i in seq_along(gal4s))
    {
    gal4 <- gal4s[i]
    
    # summarize medians
    for (j in seq_along(glist[[gal4]]))
      {
      temp <- data.frame(glist[[gal4]][[j]]$median)
      colnames(temp) <- "median"
      temp["group"] <- names(glist[[gal4]])[j]
      if(j==1) pData <- temp else pData <- rbind(pData,temp)
      }
    pData$group <- factor(pData$group,levels=unique(pData$group),labels=unique(pData$group)) # correct order

    # calculate breaks on y axis
    yb <- (max(pData$median) - min(pData$median))/minBreaks
    if(yb>0.1) yb <- trunc(yb*10)/10 else if (yb>0.01) yb <- trunc(yb*100)/100 else yb <- trunc(yb*1000)/1000

    # plot boxplots of medians
    ggplot(pData, aes(x=group,y=median)) +geom_boxplot(aes(fill=group),size=I(1.5)) +
      scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio Antibody/BrpCT")) +
      theme_bw() +ggtitle(gal4) +scale_y_continuous(breaks=seq(0,10,yb)) +
      theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5),panel.grid.major.x=element_blank(),panel.grid.minor=element_blank())
    ggsave(paste0(dDir,"plots/",gal4,"_medians.pdf"))  
    }
  }

# Plot median ratios of antibodies per GAL4/GFP line as boxplots
boxplot_ab <- function(dDir, abList, glist, minBreaks=5, invert=F) # dDir is the base directory where plots are saved
  {
  for (i in seq_along(abList))
    {
    ab <- abList[i]

    # summarize medians
    for (j in seq_along(glist))
      {
      if(any(names(glist[[j]])==ab))
        {
        temp <- data.frame(glist[[j]][[ab]]$median)
        colnames(temp) <- "median"
        temp["group"] <- names(glist)[[j]]
        if(j==1) pData <- temp else pData <- rbind(pData,temp)
        }
      }
    pData$group <- factor(pData$group,levels=unique(pData$group),labels=unique(pData$group)) # correct order
    if(invert) pData$median <- 1/pData$median
   
    # calculate breaks on y axis
    yb <- (max(pData$median) - min(pData$median))/minBreaks
    if(yb>0.1) yb <- trunc(yb*10)/10 else if (yb>0.01) yb <- trunc(yb*100)/100 else yb <- trunc(yb*1000)/1000
    
    # plot boxplots of medians
    if(invert)
      {
      ggplot(pData, aes(x=group,y=median)) +geom_boxplot(aes(fill=group),size=I(1.5)) +
        scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio BrpCT/",ab)) +
        theme_bw() +ggtitle(ab) +scale_y_continuous(breaks=seq(0,10,yb)) +
        theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5),panel.grid.major.x=element_blank(),panel.grid.minor=element_blank())
      } else
      {
      ggplot(pData, aes(x=group,y=median)) +geom_boxplot(aes(fill=group),size=I(1.5)) +
        scale_fill_brewer(palette="Set1") +ylab(paste0("Relative enrichment of median ratio ",ab,"/BrpCT")) +
        theme_bw() +ggtitle(ab) +scale_y_continuous(breaks=seq(0,10,yb)) +
        theme(legend.position="none", axis.title.x=element_blank(), panel.grid.major=element_line(colour="grey60"),plot.title=element_text(hjust=0.5),panel.grid.major.x=element_blank(),panel.grid.minor=element_blank())
      }
    ggsave(paste0(dDir,"plots/",ab,"_medians.pdf"))  
    }
  }
