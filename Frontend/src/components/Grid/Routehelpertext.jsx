import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Typography from "@material-ui/core/Typography";
import Grid from "@material-ui/core/Grid";

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

export default function Rouotehelpertext() {
  const classes = useStyles();

  return (
    <Grid container spacing={1} className={classes.root}>
      <Grid item xs={12}>
        <Typography className={classes.pos} color="textSecondary">
          Displays information to the current
          <span style={{ fontWeight: "600" }}> Route</span> such as departure,
          arrival, road type and local weather.
        </Typography>
      </Grid>

      <Grid item xs={12}></Grid>
    </Grid>
  );
}
