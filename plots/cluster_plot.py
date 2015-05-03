import numpy as np
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
import sys

struct_to_plot = int(sys.argv[1])
index = 0
color = ["red", "green", "blue", "yellow", "orange", "black", "white", "purple", "brown", "grey"]

def parseLine(l):
    clusters = l.split(":")
    retVal = []
    for i in range(0, len(clusters)):
        vals = clusters[i].split(" ")
        for j in range(0, len(vals)/4):
            point = []
            point.append(float(vals[j*4+1]))
            point.append(float(vals[j*4+2]))
            point.append(float(vals[j*4+3]))
            point.append(color[i%len(color)])
            retVal.append(point)

    return retVal

def plotMyStructure(l, i):
    points = parseLine(l)
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    fig.canvas.set_window_title(str(i)+" clusters")

    minValue = sys.maxint
    maxValue = -sys.maxint

    for p in points:
        x = p[0]
        y = p[1]
        z = p[2]
        ax.scatter(x, y, z, c=p[3], marker="o")
        maxValue = max(maxValue, x, y, z)
        minValue = min(minValue, x, y, z)

    ax.set_xlabel('X Label')
    ax.set_autoscaley_on(False)
    ax.set_ylabel('Y Label')
    ax.set_zlabel('Z Label')
    ax.set_zlim3d(minValue, maxValue)
    ax.set_ylim3d(minValue, maxValue)
    ax.set_xlim3d(minValue, maxValue)



