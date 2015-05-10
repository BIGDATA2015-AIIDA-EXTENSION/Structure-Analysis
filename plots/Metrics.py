class Metrics:

    def __init__(self, json):
        self.metrics = []
        for jsonMetric in json["metrics"]:
            self.metrics.append(Metric(jsonMetric))

    def plot(self, plt):
        fig = plt.figure()
        ax = fig.add_subplot(111)
        ax.set_ylim(0, 10)
        
        for metric in self.metrics:
            metric.plot(ax)


class Metric:

    def __init__(self, json):
        self.points = []
        self.name = json["name"]
        for point in json["values"]:
            self.points.append((point["x"], point["y"]))

    def plot(self, ax):
    	(listx, listy) = zip(* self.points)
        ax.plot(listx, listy, label=self.name)
