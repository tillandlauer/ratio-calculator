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
generateHisto <- function(data, ratios, bins, manual=F) # the manual flag is deprecated and will be removed
  {
  merged <- merge(ratios,data,by.x="finalrank",by.y="X")
  merged <- merged[-which(duplicated(merged$finalrank)),]

  stepList <- seq(log2(1/256),log2(256),2*log2(256)/bins)
  if(manual) 
    {
    data <- merged[with(merged,order(merged$ratio.x)),]
    data["bin"] <- cut(log2(data$ratio.x),stepList,include.lowest=T,labels=seq(1,bins,1))
    data["cut"] <- cut(log2(data$ratio.x),stepList,include.lowest=T)
    data <- data[,c(1,6,8:ncol(data))]
  } else
    {
    data <- merged[with(merged,order(merged$ratio)),]
    data["bin"] <- cut(log2(data$ratio),stepList,include.lowest=T,labels=seq(1,bins,1))
    data["cut"] <- cut(log2(data$ratio),stepList,include.lowest=T)
    data <- data[,c(1,6:ncol(data))]
    }
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
  
  ggplot(histo, aes(x=ori_ratio,y=freq)) +geom_bar(stat="identity",fill="forestgreen") +
  	scale_x_continuous(trans='log2', breaks=c(1/16,1/8,1/4,1/2,1,2,4,8,16), labels=c("1/16","1/8","1/4","1/2","1","2","4","8","16")) +
  	scale_y_continuous(breaks=c(seq(-1,1,0.005))) +coord_cartesian(xlim=c(1/display,display),ylim=c(-0.03,0.03)) +
  	xlab(paste("Ratio",gal4,ab)) +ylab("Difference in Normalized Frequency") +theme_bw() +
  	theme(axis.text.y=element_blank(), panel.grid.major=element_line(colour="grey60"))
  ggsave(paste0(dDir,"plots/",gal4,"_",ab,"_difference.pdf"))
  }

# read GAL4/GFP files only (no reference) and bin the data (for manual plotting)
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

# calculate normalized quantile ratios for several GAL4 lines
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

# calculate quantile ratios, normalize GAL4/GFP ratios by reference neuropil 
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
    qmin <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=1)/wtd_quant(all$ratio,weight=all[,(6+i)],q=1)
    q1 <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0.75)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0.75)
    qmed <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0.5)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0.5)
    q3 <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0.25)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0.25)
    qmax <- wtd_quant(aGFP$ratio,weight=aGFP[,(6+i)],q=0)/wtd_quant(all$ratio,weight=all[,(6+i)],q=0)

    temp <- data.frame(t(c(qmin,q1,qmed,q3,qmax)))
    colnames(temp) <- c("min","q25","median","q75","max")
    if(i==1) dV <- temp else dV <- rbind(dV,temp)
    }

  return(dV)
  }

# The following method is described in the book Relative Distribution Methods in the Social Sciences by Handcock and Morris, copyright 1999 Springer-Verlag, Inc., ISBN 0387987789, and is transmitted by permission of Springer-Verlag, Inc. 
# Copyright (c) 1999 Mark S. Handcock 
wtd_quant <- function (x, q = 0.5, na.rm = FALSE, weight = FALSE) # function without modifications from reldist 1.6.4, removed in reldist 1.6.6
  {
    if (mode(x) != "numeric") 
        stop("need numeric data")
    x <- as.vector(x)
    wnas <- is.na(x)
    if (sum(wnas) > 0) {
        if (na.rm) 
            x <- x[!wnas]
        if (!missing(weight)) {
            weight <- weight[!wnas]
        }
        else return(NA)
    }
    n <- length(x)
    half <- (n + 1)/2
    if (n%%2 == 1) {
        if (!missing(weight)) {
            weight <- weight/sum(weight)
            sx <- sort.list(x)
            sweight <- cumsum(weight[sx])
            min(x[sx][sweight >= q])
        }
        else {
            x[order(x)[half]]
        }
    }
    else {
        if (!missing(weight)) {
            weight <- weight/sum(weight)
            sx <- sort.list(x)
            sweight <- cumsum(weight[sx])
            min(x[sx][sweight >= q])
        }
        else {
            half <- floor(half) + 0:1
            sum(x[order(x)[half]])/2
        }
    }
  }
