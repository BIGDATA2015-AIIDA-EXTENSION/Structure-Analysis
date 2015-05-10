import json
import sys

color = ["red", "green", "blue", "yellow", "orange", "black", "white", "purple", "brown", "grey"]


class Clustering:

    def __init__(self, id, json):
    	self.clusters = []
        self.id = id
        for jsonCluster in json:
            self.clusters.append(Cluster(jsonCluster))

    def plot(self, plt):
        cluster_number = 0
        fig = plt.figure()
        ax = fig.add_subplot(111, projection='3d')
        fig.canvas.set_window_title(self.id+" with "+str(len(self.clusters))+" clusters")
        minValue = sys.maxint
        maxValue = -sys.maxint

        for cluster in self.clusters:
            cluster_number +=1
            cluster_color = color[cluster_number % len(color)]

            for point in cluster.points:
                x = point[0]
                y = point[1]
                z = point[2]
                ax.scatter(x, y, z, c=cluster_color, marker="o")
                maxValue = max(maxValue, x, y, z)
                minValue = min(minValue, x, y, z)

        ax.set_xlabel('X Label')
        ax.set_autoscaley_on(False)
        ax.set_ylabel('Y Label')
        ax.set_zlabel('Z Label')
        ax.set_zlim3d(minValue, maxValue)
        ax.set_ylim3d(minValue, maxValue)
        ax.set_xlim3d(minValue, maxValue)



class Cluster:

    def __init__(self, json):
    	self.points = []

        for point in json:
            self.points.append(point)


