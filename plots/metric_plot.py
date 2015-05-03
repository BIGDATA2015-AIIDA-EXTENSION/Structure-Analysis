__author__ = 'lukas'
import numpy as np
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
import sys


def parseMetrics(l):
    metric = []
    values = l.split(" ")
    for j in range(0, len(values)):
        metric.append(float(values[j]))

    return metric


def plotMetrics(l):
    mets = l.split(":")
    fig = plt.figure()
    ax1 = fig.add_subplot(111)

    for m in mets:

        vals = parseMetrics(m)
        print(vals)
        ax1.plot(range(1, len(vals)+1), vals)
