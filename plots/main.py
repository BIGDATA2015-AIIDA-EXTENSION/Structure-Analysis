__author__ = 'lukas'
import numpy as np
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
import sys
import metric_plot as mp
import cluster_plot as pp


def plotStructure(l):
	s = l.split("|")

	if len(s) == 2:
		mp.plotMetrics(s[1])

	i = 1
	for p in s[0].split(";"):
		pp.plotMyStructure(p, i)
		i+=1

	plt.show()


struct_to_plot = int(sys.argv[1])
index = 0
with open('cluster.txt') as fp:
    for line in fp:
        if struct_to_plot == index:
            plotStructure(line)
        index +=1