import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Typography from "@material-ui/core/Typography";
import Grid from "@material-ui/core/Grid";
import TrendingFlatIcon from "@material-ui/icons/TrendingFlat";

const useStyles = makeStyles({
  root: {
    minWidth: 275,
    maxWidth: 400,
  },
  bullet: {
    display: "inline-block",
    margin: "0 2px",
    transform: "scale(0.8)",
  },
  title: {
    fontSize: 14,
  },
  pos: {
    marginBottom: 12,
  },
});

export default function Behaviourhelpertext() {
  const classes = useStyles();

  return (
    <Grid container spacing={1} className={classes.root}>
      <Grid item xs={12}>
        <Typography className={classes.pos} color="textSecondary">
          <span style={{ fontWeight: "600" }}>Ecosstatus</span> is a key figure
          calculated from several factors e.g. Speed, Acceleration, etc. , which
          indicates the driving behaviour of the truck driver.
        </Typography>
      </Grid>
      <Grid item xs={2}>
        <TrendingFlatIcon
          style={{ fontWeight: "500", transform: "rotate(0deg)" }}
        ></TrendingFlatIcon>
      </Grid>
      <Grid
        item
        xs={10}
        style={{
          fontWeight: "500",
        }}
      >
        Normal Behaviour
        <Typography variant="body2" component="p" color="textSecondary">
          Behaviour does not have to be adapted.
          {/* <br />
          {'"a benevolent smile"'} */}
        </Typography>
      </Grid>
      <Grid item xs={2}>
        <TrendingFlatIcon
          style={{
            fontWeight: "500",
            transform: "rotate(45deg)",
            color: "#ff9800",
          }}
        ></TrendingFlatIcon>
      </Grid>
      <Grid
        item
        xs={10}
        style={{
          fontWeight: "500",
          color: "#ff9800",
        }}
      >
        Conspicuous Behaviour
        <Typography variant="body2" component="p" color="textSecondary">
          The driving behaviour should be improved.
          {/* <br />
          {'"a benevolent smile"'} */}
        </Typography>
      </Grid>
      <Grid item xs={2}>
        <TrendingFlatIcon
          style={{
            fontWeight: "500",
            transform: "rotate(90deg)",
            color: "#f44336",
          }}
        ></TrendingFlatIcon>
      </Grid>
      <Grid
        item
        xs={10}
        style={{
          fontWeight: "500",
          color: "#f44336",
        }}
      >
        Bad Behaviour
        <Typography variant="body2" component="p" color="textSecondary">
          Driving behaviour urgently needs to be improved.
          {/* <br />
          {'"a benevolent smile"'} */}
        </Typography>
      </Grid>

      <Typography variant="h5" component="h2"></Typography>

      <Grid item xs={12}></Grid>
    </Grid>
  );
}
