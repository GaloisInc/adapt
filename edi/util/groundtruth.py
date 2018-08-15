


class GroundTruth:

    def __init__(self, reader, ty=None):
        self.header = next(reader)[1:]
        def match(row):
            if ty != None:
                return row[1] == ty
            else:
                return True
        self.data = {row[0]
                     for row in reader
                     if match(row)}
