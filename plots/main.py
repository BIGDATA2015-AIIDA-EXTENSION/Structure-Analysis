__author__ = 'lukas'
import numpy as np
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
import sys
from Clustering import Clustering
from Metrics import Metrics
import json


def clusteringList(json):
    clustering = []
    print(json["id"])
    print(json["info"])
    for jsonClustering in json["clusterings"]:
        clustering.append(Clustering(json["id"], jsonClustering))
    return clustering


struct_to_plot = int(sys.argv[1])
index = 0
with open('cluster.txt') as fp:
    for line in fp:
        if struct_to_plot == index:
            json = json.loads(line)

            for clustering in clusteringList(json):
                clustering.plot(plt)

            Metrics(json).plot(plt)


            plt.show()
        index +=1